package net.radiovoid.bbs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/* JADX INFO: loaded from: classes.dex */
public class PlayerService extends Service {
    public static final String ACT_NEXT = "rv.NEXT";
    public static final String ACT_PLAY_PAUSE = "rv.PLAY_PAUSE";
    public static final String ACT_PREV = "rv.PREV";
    public static final String ACT_STOP = "rv.STOP";
    private static final String CHANNEL = "rv_playback";
    private static final LinkedList<String> LOG = new LinkedList<>();
    private static final int NOTIF_ID = 1;
    private ModEngine engine;
    private AudioFocusRequest focusReq;
    private Listener listener;
    private long loadToken;
    private MediaPlayer mp;
    private SharedPreferences prefs;
    private boolean preparing;
    public boolean repeat;
    private MediaSession session;
    public boolean shuffle;
    private Visualizer viz;
    private PowerManager.WakeLock wakeLock;
    private boolean wasPlayingBeforeFocusLoss;
    private WifiManager.WifiLock wifiLock;
    private final IBinder binder = new LocalBinder();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private final List<Track> queue = new ArrayList();
    private int index = -1;
    private final Runnable chatWatch = new Runnable() { // from class: net.radiovoid.bbs.PlayerService.2
        @Override // java.lang.Runnable
        public void run() {
            if (PlayerService.this.listener == null) {
                ChatJobService.checkAndNotify(PlayerService.this, null);
            }
            PlayerService.this.main.postDelayed(this, 45000L);
        }
    };

    public interface Listener {
        void onError(String str);

        void onPcm(short[] sArr, int i);

        void onState();
    }

    public static synchronized void log(String str) {
        LinkedList<String> linkedList = LOG;
        linkedList.add(String.format(Locale.US, "%tT %s", Long.valueOf(System.currentTimeMillis()), str));
        if (linkedList.size() > 80) {
            linkedList.removeFirst();
        }
    }

    public static synchronized String dumpLog() {
        LinkedList<String> linkedList;
        linkedList = LOG;
        return linkedList.isEmpty() ? "(log empty)" : String.join("\n", linkedList);
    }

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        public PlayerService get() {
            return PlayerService.this;
        }
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        SharedPreferences sharedPreferences = getSharedPreferences("rv", 0);
        this.prefs = sharedPreferences;
        this.shuffle = sharedPreferences.getBoolean("shuffle", false);
        this.repeat = this.prefs.getBoolean("repeat", false);
        ((NotificationManager) getSystemService(NotificationManager.class)).createNotificationChannel(new NotificationChannel(CHANNEL, "Playback", 2));
        MediaSession mediaSession = new MediaSession(this, "RadioVoid");
        this.session = mediaSession;
        mediaSession.setFlags(3);
        this.session.setSessionActivity(PendingIntent.getActivity(this, 9, new Intent(this, (Class<?>) MainActivity.class), 67108864));
        this.session.setCallback(new MediaSession.Callback() { // from class: net.radiovoid.bbs.PlayerService.1
            @Override // android.media.session.MediaSession.Callback
            public void onPlay() {
                if (PlayerService.this.isPlaying()) {
                    return;
                }
                PlayerService.this.playPause();
            }

            @Override // android.media.session.MediaSession.Callback
            public void onPause() {
                if (PlayerService.this.isPlaying()) {
                    PlayerService.this.playPause();
                }
            }

            @Override // android.media.session.MediaSession.Callback
            public void onSkipToNext() {
                PlayerService.this.next(true);
            }

            @Override // android.media.session.MediaSession.Callback
            public void onSkipToPrevious() {
                PlayerService.this.prev();
            }

            @Override // android.media.session.MediaSession.Callback
            public void onStop() {
                PlayerService.this.stopPlayback();
            }

            @Override // android.media.session.MediaSession.Callback
            public void onSeekTo(long j) {
                PlayerService.this.seekTo((int) j);
            }
        });
        this.session.setActive(true);
        this.wakeLock = ((PowerManager) getSystemService(PowerManager.class)).newWakeLock(1, "radiovoid:playback");
        this.wifiLock = ((WifiManager) getApplicationContext().getSystemService("wifi")).createWifiLock(3, "radiovoid:stream");
        this.main.postDelayed(this.chatWatch, 30000L);
        log("service created");
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            action.hashCode();
            switch (action) {
                case "rv.PLAY_PAUSE":
                    playPause();
                    break;
                case "rv.NEXT":
                    next(true);
                    break;
                case "rv.PREV":
                    prev();
                    break;
                case "rv.STOP":
                    stopPlayback();
                    break;
            }
        }
        return 1;
    }

    @Override // android.app.Service
    public void onTaskRemoved(Intent intent) {
        stopPlayback();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public List<Track> getQueue() {
        return this.queue;
    }

    public Track current() {
        int i = this.index;
        if (i < 0 || i >= this.queue.size()) {
            return null;
        }
        return this.queue.get(this.index);
    }

    public boolean hasTrack() {
        return current() != null;
    }

    public boolean isPreparing() {
        return this.preparing;
    }

    public boolean isPlaying() {
        ModEngine modEngine = this.engine;
        if (modEngine != null) {
            return !modEngine.isPaused();
        }
        try {
            MediaPlayer mediaPlayer = this.mp;
            return mediaPlayer != null && mediaPlayer.isPlaying();
        } catch (Exception unused) {
            return false;
        }
    }

    public int positionMs() {
        ModEngine modEngine = this.engine;
        if (modEngine != null) {
            return modEngine.positionMs();
        }
        try {
            MediaPlayer mediaPlayer = this.mp;
            if (mediaPlayer == null || this.preparing) {
                return 0;
            }
            return mediaPlayer.getCurrentPosition();
        } catch (Exception unused) {
            return 0;
        }
    }

    public int durationMs() {
        ModEngine modEngine = this.engine;
        if (modEngine != null) {
            return modEngine.durationMs();
        }
        try {
            MediaPlayer mediaPlayer = this.mp;
            if (mediaPlayer != null && !this.preparing) {
                return Math.max(mediaPlayer.getDuration(), 0);
            }
        } catch (Exception unused) {
        }
        Track trackCurrent = current();
        if (trackCurrent != null) {
            return (int) (trackCurrent.duration * 1000.0d);
        }
        return 0;
    }

    public void setShuffle(boolean z) {
        this.shuffle = z;
        this.prefs.edit().putBoolean("shuffle", z).apply();
    }

    public void setRepeat(boolean z) {
        this.repeat = z;
        this.prefs.edit().putBoolean("repeat", z).apply();
    }

    public void setQueueAndPlay(List<Track> list, int i) {
        this.queue.clear();
        this.queue.addAll(list);
        play(i, false, 0);
    }

    public void setQueueAndResume(List<Track> list, int i, int i2) {
        this.queue.clear();
        this.queue.addAll(list);
        play(i, true, i2);
    }

    public void play(int i) {
        play(i, false, 0);
    }

    private void play(int i, final boolean z, final int i2) {
        String str;
        if (i < 0 || i >= this.queue.size()) {
            return;
        }
        final Track track = this.queue.get(i);
        StringBuilder sb = new StringBuilder("play ");
        sb.append(track.title);
        sb.append(" (");
        sb.append(track.ext());
        sb.append(")");
        if (z) {
            str = " [resume@" + i2 + "]";
        } else {
            str = "";
        }
        sb.append(str);
        log(sb.toString());
        if (!track.isPlayable()) {
            err("[ " + track.ext().toUpperCase(Locale.US) + " FORMAT - WEB TERMINAL ONLY ]");
            return;
        }
        try {
            startService(new Intent(this, (Class<?>) PlayerService.class));
        } catch (Exception unused) {
        }
        stopEngines();
        this.index = i;
        this.preparing = true;
        notifyState();
        final long j = this.loadToken + 1;
        this.loadToken = j;
        if (track.isModule()) {
            Net.runIo(new Runnable() { // from class: net.radiovoid.bbs.PlayerService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    PlayerService.this.m95lambda$play$2$netradiovoidbbsPlayerService(track, i2, j, z);
                }
            });
            return;
        }
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            this.mp = mediaPlayer;
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(1).setContentType(2).build());
            this.mp.setWakeMode(this, 1);
            File fileCached = Net.cached(this, track);
            if (fileCached != null) {
                this.mp.setDataSource(fileCached.getPath());
                log("playing from cache");
            } else {
                this.mp.setDataSource(track.url);
                Net.prefetch(this, track);
            }
            this.mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() { // from class: net.radiovoid.bbs.PlayerService$$ExternalSyntheticLambda1
                @Override // android.media.MediaPlayer.OnPreparedListener
                public final void onPrepared(MediaPlayer mediaPlayer2) {
                    PlayerService.this.m96lambda$play$3$netradiovoidbbsPlayerService(j, i2, z, mediaPlayer2);
                }
            });
            this.mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { // from class: net.radiovoid.bbs.PlayerService$$ExternalSyntheticLambda2
                @Override // android.media.MediaPlayer.OnCompletionListener
                public final void onCompletion(MediaPlayer mediaPlayer2) {
                    PlayerService.this.m97lambda$play$4$netradiovoidbbsPlayerService(j, mediaPlayer2);
                }
            });
            this.mp.setOnErrorListener(new MediaPlayer.OnErrorListener() { // from class: net.radiovoid.bbs.PlayerService$$ExternalSyntheticLambda3
                @Override // android.media.MediaPlayer.OnErrorListener
                public final boolean onError(MediaPlayer mediaPlayer2, int i3, int i4) {
                    return PlayerService.this.m98lambda$play$5$netradiovoidbbsPlayerService(j, mediaPlayer2, i3, i4);
                }
            });
            this.mp.prepareAsync();
        } catch (Exception e) {
            log("stream FAIL: " + e);
            this.preparing = false;
            err("PLAY FAILED: " + e.getMessage());
            notifyState();
        }
    }

    /* JADX INFO: renamed from: lambda$play$2$net-radiovoid-bbs-PlayerService, reason: not valid java name */
    /* synthetic */ void m95lambda$play$2$netradiovoidbbsPlayerService(Track track, int i, final long j, final boolean z) {
        Exception e;
        OpenmptEngine openmptEngine;
        try {
            byte[] bArrDownloadFileBlocking = Net.downloadFileBlocking(this, track);
            log("module downloaded: " + bArrDownloadFileBlocking.length + " bytes");
            AnonymousClass3 anonymousClass3 = new AnonymousClass3();
            ModEngine ozmodEngine = null;
            if (Openmpt.available()) {
                try {
                    openmptEngine = new OpenmptEngine(bArrDownloadFileBlocking, anonymousClass3);
                    try {
                        log("engine: libopenmpt");
                    } catch (Exception e2) {
                        e = e2;
                        log("libopenmpt parse failed, falling back: " + e.getMessage());
                    }
                } catch (Exception e3) {
                    e = e3;
                    openmptEngine = null;
                }
                ozmodEngine = openmptEngine;
            }
            if (ozmodEngine == null) {
                ozmodEngine = track.ext().equals("it") ? new OzmodEngine(bArrDownloadFileBlocking, (int) (track.duration * 1000.0d), anonymousClass3) : new IbxmEngine(bArrDownloadFileBlocking, anonymousClass3);
                log("engine: " + ozmodEngine.getClass().getSimpleName());
            }
            final ModEngine modEngine = ozmodEngine;
            if (i > 0 && modEngine.canSeek()) {
                modEngine.seekMs(i);
            }
            this.main.post(new Runnable() { // from class: net.radiovoid.bbs.PlayerService$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    PlayerService.this.m93lambda$play$0$netradiovoidbbsPlayerService(j, modEngine, z);
                }
            });
        } catch (Throwable th) {
            log("module FAIL: " + th);
            this.main.post(new Runnable() { // from class: net.radiovoid.bbs.PlayerService$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    PlayerService.this.m94lambda$play$1$netradiovoidbbsPlayerService(j, th);
                }
            });
        }
    }

    /* JADX INFO: renamed from: net.radiovoid.bbs.PlayerService$3, reason: invalid class name */
    class AnonymousClass3 implements ModEngine.Callback {
        AnonymousClass3() {
        }

        /* JADX INFO: renamed from: lambda$onComplete$0$net-radiovoid-bbs-PlayerService$3, reason: not valid java name */
        /* synthetic */ void m100lambda$onComplete$0$netradiovoidbbsPlayerService$3() {
            PlayerService.this.next(false);
        }

        @Override // net.radiovoid.bbs.ModEngine.Callback
        public void onComplete() {
            PlayerService.this.main.post(new Runnable() { // from class: net.radiovoid.bbs.PlayerService$3$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    AnonymousClass3.this.m100lambda$onComplete$0$netradiovoidbbsPlayerService$3();
                }
            });
        }

        @Override // net.radiovoid.bbs.ModEngine.Callback
        public void onPcm(short[] sArr, int i) {
            Listener listener = PlayerService.this.listener;
            if (listener != null) {
                listener.onPcm(sArr, i);
            }
        }
    }

    /* JADX INFO: renamed from: lambda$play$0$net-radiovoid-bbs-PlayerService, reason: not valid java name */
    /* synthetic */ void m93lambda$play$0$netradiovoidbbsPlayerService(long j, ModEngine modEngine, boolean z) {
        if (j != this.loadToken) {
            modEngine.release();
            return;
        }
        this.engine = modEngine;
        this.preparing = false;
        boolean z2 = true;
        boolean z3 = z || requestFocus();
        log("module ready, focus=" + z3);
        ModEngine modEngine2 = this.engine;
        if (!z && z3) {
            z2 = false;
        }
        modEngine2.start(z2);
        if (!z && z3) {
            acquireLocks(false);
        }
        savePosition();
        notifyState();
        prefetchNext();
    }

    /* JADX INFO: renamed from: lambda$play$1$net-radiovoid-bbs-PlayerService, reason: not valid java name */
    /* synthetic */ void m94lambda$play$1$netradiovoidbbsPlayerService(long j, Throwable th) {
        if (j != this.loadToken) {
            return;
        }
        this.preparing = false;
        err("MODULE: " + th.getMessage());
        notifyState();
    }

    /* JADX INFO: renamed from: lambda$play$3$net-radiovoid-bbs-PlayerService, reason: not valid java name */
    /* synthetic */ void m96lambda$play$3$netradiovoidbbsPlayerService(long j, int i, boolean z, MediaPlayer mediaPlayer) {
        if (j != this.loadToken) {
            return;
        }
        this.preparing = false;
        if (i > 0) {
            try {
                mediaPlayer.seekTo(i);
            } catch (Exception unused) {
            }
        }
        if (!z && requestFocus()) {
            mediaPlayer.start();
            acquireLocks(true);
        }
        log("stream ready".concat(z ? " (paused)" : ""));
        attachVisualizer();
        savePosition();
        notifyState();
        prefetchNext();
    }

    /* JADX INFO: renamed from: lambda$play$4$net-radiovoid-bbs-PlayerService, reason: not valid java name */
    /* synthetic */ void m97lambda$play$4$netradiovoidbbsPlayerService(long j, MediaPlayer mediaPlayer) {
        if (j == this.loadToken) {
            next(false);
        }
    }

    /* JADX INFO: renamed from: lambda$play$5$net-radiovoid-bbs-PlayerService, reason: not valid java name */
    /* synthetic */ boolean m98lambda$play$5$netradiovoidbbsPlayerService(long j, MediaPlayer mediaPlayer, int i, int i2) {
        log("stream error " + i + "/" + i2);
        if (j != this.loadToken) {
            return true;
        }
        this.preparing = false;
        err("STREAM ERROR " + i + "/" + i2);
        notifyState();
        return true;
    }

    public void playPause() {
        ModEngine modEngine = this.engine;
        if (modEngine != null) {
            if (modEngine.isPaused()) {
                if (requestFocus()) {
                    this.engine.resume();
                    acquireLocks(false);
                }
            } else {
                this.engine.pause();
                releaseLocks();
                savePosition();
            }
        } else {
            MediaPlayer mediaPlayer = this.mp;
            if (mediaPlayer != null && !this.preparing) {
                try {
                    if (mediaPlayer.isPlaying()) {
                        this.mp.pause();
                        releaseLocks();
                        savePosition();
                    } else if (requestFocus()) {
                        this.mp.start();
                        acquireLocks(true);
                    }
                } catch (Exception unused) {
                }
            } else if (!this.queue.isEmpty() && this.index < 0) {
                play(0, false, 0);
                return;
            }
        }
        notifyState();
    }

    public void next(boolean z) {
        int size;
        if (this.queue.isEmpty()) {
            return;
        }
        if (this.repeat && !z) {
            play(this.index, false, 0);
            return;
        }
        if (this.shuffle) {
            size = this.queue.size() > 1 ? this.rnd.nextInt(this.queue.size()) : 0;
            if (size == this.index) {
                size = (size + 1) % this.queue.size();
            }
        } else {
            int i = this.index + 1;
            if (i < this.queue.size()) {
                size = i;
            } else {
                if (!z) {
                    pauseAtEnd();
                    return;
                }
                size = 0;
            }
        }
        int i2 = 0;
        while (!this.queue.get(size).isPlayable()) {
            int i3 = i2 + 1;
            if (i2 >= this.queue.size()) {
                break;
            }
            size = (size + 1) % this.queue.size();
            i2 = i3;
        }
        play(size, false, 0);
    }

    private void prefetchNext() {
        if (this.queue.isEmpty() || this.shuffle) {
            return;
        }
        List<Track> list = this.queue;
        Net.prefetch(this, list.get((this.index + 1) % list.size()));
    }

    private void pauseAtEnd() {
        if (isPlaying()) {
            playPause();
        }
        seekTo(0);
        savePosition();
    }

    public void prev() {
        if (this.queue.isEmpty()) {
            return;
        }
        if (positionMs() > 3000) {
            seekTo(0);
            return;
        }
        int size = this.index - 1;
        if (size < 0) {
            size = this.queue.size() - 1;
        }
        int i = 0;
        while (!this.queue.get(size).isPlayable()) {
            int i2 = i + 1;
            if (i >= this.queue.size()) {
                break;
            }
            size--;
            if (size < 0) {
                size = this.queue.size() - 1;
            }
            i = i2;
        }
        play(size, false, 0);
    }

    public void seekTo(int i) {
        ModEngine modEngine = this.engine;
        if (modEngine == null) {
            MediaPlayer mediaPlayer = this.mp;
            if (mediaPlayer != null && !this.preparing) {
                try {
                    mediaPlayer.seekTo(i);
                } catch (Exception unused) {
                }
            }
        } else if (modEngine.canSeek()) {
            this.engine.seekMs(i);
        }
        notifyState();
    }

    public void stopPlayback() {
        savePosition();
        stopEngines();
        this.preparing = false;
        notifyState();
        stopForeground(1);
        stopSelf();
    }

    public void savePosition() {
        Track trackCurrent = current();
        if (trackCurrent == null) {
            return;
        }
        this.prefs.edit().putString("last_track_id", trackCurrent.id).putInt("last_pos", positionMs()).apply();
    }

    public String savedTrackId() {
        return this.prefs.getString("last_track_id", null);
    }

    public int savedPos() {
        return this.prefs.getInt("last_pos", 0);
    }

    private void stopEngines() {
        this.loadToken++;
        Visualizer visualizer = this.viz;
        if (visualizer != null) {
            try {
                visualizer.release();
            } catch (Exception unused) {
            }
            this.viz = null;
        }
        ModEngine modEngine = this.engine;
        if (modEngine != null) {
            modEngine.release();
            this.engine = null;
        }
        MediaPlayer mediaPlayer = this.mp;
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception unused2) {
            }
            this.mp = null;
        }
        releaseLocks();
    }

    private void acquireLocks(boolean z) {
        if (!this.wakeLock.isHeld()) {
            this.wakeLock.acquire(14400000L);
        }
        if (!z || this.wifiLock.isHeld()) {
            return;
        }
        this.wifiLock.acquire();
    }

    private void releaseLocks() {
        if (this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }
        if (this.wifiLock.isHeld()) {
            this.wifiLock.release();
        }
    }

    private void attachVisualizer() {
        if (checkSelfPermission("android.permission.RECORD_AUDIO") != 0) {
            return;
        }
        try {
            Visualizer visualizer = new Visualizer(this.mp.getAudioSessionId());
            this.viz = visualizer;
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            this.viz.setDataCaptureListener(new Visualizer.OnDataCaptureListener() { // from class: net.radiovoid.bbs.PlayerService.4
                @Override // android.media.audiofx.Visualizer.OnDataCaptureListener
                public void onFftDataCapture(Visualizer visualizer2, byte[] bArr, int i) {
                }

                @Override // android.media.audiofx.Visualizer.OnDataCaptureListener
                public void onWaveFormDataCapture(Visualizer visualizer2, byte[] bArr, int i) {
                    Listener listener = PlayerService.this.listener;
                    if (listener == null) {
                        return;
                    }
                    short[] sArr = new short[bArr.length * 2];
                    for (int i2 = 0; i2 < bArr.length; i2++) {
                        short s = (short) (((bArr[i2] & 255) - 128) << 8);
                        int i3 = i2 * 2;
                        sArr[i3] = s;
                        sArr[i3 + 1] = s;
                    }
                    listener.onPcm(sArr, bArr.length);
                }
            }, Visualizer.getMaxCaptureRate(), true, false);
            this.viz.setEnabled(true);
        } catch (Exception e) {
            log("visualizer unavailable: " + e.getMessage());
            this.viz = null;
        }
    }

    private boolean requestFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(AudioManager.class);
        if (this.focusReq == null) {
            this.focusReq = new AudioFocusRequest.Builder(1).setAudioAttributes(new AudioAttributes.Builder().setUsage(1).setContentType(2).build()).setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener() { // from class: net.radiovoid.bbs.PlayerService$$ExternalSyntheticLambda6
                @Override // android.media.AudioManager.OnAudioFocusChangeListener
                public final void onAudioFocusChange(int i) {
                    PlayerService.this.m99lambda$requestFocus$6$netradiovoidbbsPlayerService(i);
                }
            }).build();
        }
        boolean z = audioManager.requestAudioFocus(this.focusReq) == 1;
        if (!z) {
            log("audio focus DENIED");
        }
        return z;
    }

    /* JADX INFO: renamed from: lambda$requestFocus$6$net-radiovoid-bbs-PlayerService, reason: not valid java name */
    /* synthetic */ void m99lambda$requestFocus$6$netradiovoidbbsPlayerService(int i) {
        if (i == -1 || i == -2) {
            this.wasPlayingBeforeFocusLoss = isPlaying();
            if (isPlaying()) {
                playPause();
                return;
            }
            return;
        }
        if (i == 1 && this.wasPlayingBeforeFocusLoss && !isPlaying()) {
            playPause();
        }
    }

    private void err(final String str) {
        final Listener listener = this.listener;
        if (listener != null) {
            this.main.post(new Runnable() { // from class: net.radiovoid.bbs.PlayerService$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    listener.onError(str);
                }
            });
        }
    }

    private void notifyState() {
        int i;
        Track trackCurrent = current();
        if (trackCurrent != null) {
            this.session.setMetadata(new MediaMetadata.Builder().putString("android.media.metadata.TITLE", trackCurrent.title).putString("android.media.metadata.ARTIST", trackCurrent.artist).putLong("android.media.metadata.DURATION", durationMs()).build());
            MediaSession mediaSession = this.session;
            PlaybackState.Builder actions = new PlaybackState.Builder().setActions(823L);
            if (this.preparing) {
                i = 6;
            } else {
                i = isPlaying() ? 3 : 2;
            }
            mediaSession.setPlaybackState(actions.setState(i, positionMs(), 1.0f).build());
            showNotification(trackCurrent);
        }
        Listener listener = this.listener;
        if (listener != null) {
            listener.onState();
        }
    }

    private PendingIntent action(String str) {
        return PendingIntent.getService(this, str.hashCode(), new Intent(this, (Class<?>) PlayerService.class).setAction(str), 67108864);
    }

    private void showNotification(Track track) {
        Notification notificationBuild = new Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_stat).setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher)).setContentTitle(track.title).setContentText(track.artist + "  ·  RADIOVOID BBS").setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, (Class<?>) MainActivity.class), 67108864)).setOngoing(isPlaying()).setVisibility(1).addAction(new Notification.Action.Builder((Icon) null, "|<<", action(ACT_PREV)).build()).addAction(new Notification.Action.Builder((Icon) null, isPlaying() ? "||" : ">", action(ACT_PLAY_PAUSE)).build()).addAction(new Notification.Action.Builder((Icon) null, ">>|", action(ACT_NEXT)).build()).setStyle(new Notification.MediaStyle().setMediaSession(this.session.getSessionToken()).setShowActionsInCompactView(0, 1, 2)).build();
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(1, notificationBuild, 2);
            } else {
                startForeground(1, notificationBuild);
            }
        } catch (Exception e) {
            log("startForeground denied: " + e.getMessage());
            try {
                ((NotificationManager) getSystemService(NotificationManager.class)).notify(1, notificationBuild);
            } catch (Exception unused) {
            }
        }
    }

    @Override // android.app.Service
    public void onDestroy() {
        this.main.removeCallbacks(this.chatWatch);
        savePosition();
        stopEngines();
        this.session.release();
        super.onDestroy();
    }
}
