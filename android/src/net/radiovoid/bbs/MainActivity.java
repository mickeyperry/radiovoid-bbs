package net.radiovoid.bbs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class MainActivity extends Activity implements PlayerService.Listener {
    private static final String SCRAMBLE = "█▓▒░@#$%&*<>/\\|≡~";
    private static final String[] SORTS = {"NEWEST", "TITLE", "ARTIST", "YEAR", "RANDOM"};
    private TrackAdapter adapter;
    private AudioManager audio;
    private TextView bPlay;
    private TextView bRpt;
    private TextView bShuf;
    private boolean bound;
    private ChatAdapter chatAdapter;
    private TextView chatBtn;
    private TextView chatClr;
    private boolean chatOpen;
    private FrameLayout chatPanel;
    private JSONObject chatReplyTo;
    private LinearLayout chatReplyBar;
    private TextView chatReplyTxt;
    private String flashMsg;
    private long flashUntil;
    private long lastSeenMsg;
    private LinearLayout libBox;
    private ListView list;
    private LinearLayout mainCol;
    private Typeface mono;
    private TextView muteBtn;
    private TextView npArtist;
    private TextView npTitle;
    private SharedPreferences prefs;
    private FrameLayout progBg;
    private View progFill;
    private View progThumb;
    private FrameLayout root;
    private TextView sortBtn;
    private TextView statusLine;
    private PlayerService svc;
    private TextView sysBtn;
    private TextView tCur;
    private TextView tTot;
    private Theme th;
    private int themeIdx;
    private int tickCount;
    private TextView trkCount;
    private TextView viewBtn;
    private VizView viz;
    private TextView vizMode;
    private FrameLayout volBg;
    private View volFill;
    private TextView volPct;
    private View volThumb;
    private final List<Track> allTracks = new ArrayList();
    private final List<Track> shown = new ArrayList();
    private int sortIdx = 0;
    private String query = "";
    private String view = "ALL";
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<Runnable> themeAppliers = new ArrayList();
    private final Random rnd = new Random();
    private final List<JSONObject> chatMsgs = new ArrayList();
    private final ServiceConnection conn = new ServiceConnection() { // from class: net.radiovoid.bbs.MainActivity.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MainActivity.this.svc = ((PlayerService.LocalBinder) iBinder).get();
            MainActivity.this.svc.setListener(MainActivity.this);
            MainActivity.this.bound = true;
            MainActivity.this.maybeRestoreSession();
            MainActivity.this.onState();
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            MainActivity.this.bound = false;
            MainActivity.this.svc = null;
        }
    };
    private final Runnable chatPoll = new AnonymousClass3();
    private final Runnable ticker = new Runnable() { // from class: net.radiovoid.bbs.MainActivity.4
        @Override // java.lang.Runnable
        public void run() {
            if (MainActivity.this.svc != null && MainActivity.this.progFill != null) {
                int iPositionMs = MainActivity.this.svc.positionMs();
                int iDurationMs = MainActivity.this.svc.durationMs();
                MainActivity.this.tCur.setText(MainActivity.mmss(iPositionMs));
                MainActivity.this.tTot.setText(MainActivity.mmss(iDurationMs));
                int width = MainActivity.this.progBg.getWidth();
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) MainActivity.this.progFill.getLayoutParams();
                layoutParams.width = iDurationMs > 0 ? (int) ((((long) width) * ((long) iPositionMs)) / ((long) iDurationMs)) : 0;
                MainActivity.this.progFill.setLayoutParams(layoutParams);
                FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) MainActivity.this.progThumb.getLayoutParams();
                layoutParams2.leftMargin = Math.max(0, Math.min(layoutParams.width - MainActivity.this.dp(4), width - MainActivity.this.dp(8)));
                MainActivity.this.progThumb.setLayoutParams(layoutParams2);
                MainActivity.this.updateVolumeUi();
                if (MainActivity.this.flashMsg != null && System.currentTimeMillis() > MainActivity.this.flashUntil) {
                    MainActivity.this.flashMsg = null;
                    MainActivity.this.statusLine.setTextColor(MainActivity.this.th.fg);
                    MainActivity.this.statusLine.setText(MainActivity.this.statusText());
                }
                if (MainActivity.access$3304(MainActivity.this) % 20 == 0 && MainActivity.this.svc.isPlaying()) {
                    MainActivity.this.svc.savePosition();
                }
            }
            MainActivity.this.ui.postDelayed(this, 500L);
        }
    };

    static /* synthetic */ int access$3304(MainActivity mainActivity) {
        int i = mainActivity.tickCount + 1;
        mainActivity.tickCount = i;
        return i;
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        SharedPreferences sharedPreferences = getSharedPreferences("rv", 0);
        this.prefs = sharedPreferences;
        this.themeIdx = sharedPreferences.getInt("theme", 0);
        this.sortIdx = this.prefs.getInt("sort", 0) % SORTS.length;
        this.view = this.prefs.getString("view", "ALL");
        this.th = Theme.ALL[this.themeIdx];
        this.mono = Typeface.createFromAsset(getAssets(), "VT323-Regular.ttf");
        setVolumeControlStream(3);
        Net.initAuth(this);
        this.lastSeenMsg = this.prefs.getLong("chat_seen", 0L);
        ArrayList arrayList = new ArrayList();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) {
            arrayList.add("android.permission.POST_NOTIFICATIONS");
        }
        if (checkSelfPermission("android.permission.RECORD_AUDIO") != 0) {
            arrayList.add("android.permission.RECORD_AUDIO");
        }
        if (!arrayList.isEmpty()) {
            requestPermissions((String[]) arrayList.toArray(new String[0]), 1);
        }
        startService(new Intent(this, (Class<?>) PlayerService.class));
        try {
            ChatJobService.ensureChannels(this);
            scheduleChatJob();
        } catch (Exception e) {
            PlayerService.log("job schedule failed: " + e);
        }
        showBoot();
        showCrashReportIfAny();
    }

    private void showCrashReportIfAny() {
        final String string = this.prefs.getString("last_crash", null);
        if (string == null) {
            return;
        }
        TextView textViewMono = mono(string, 11, Color.parseColor("#ff6666"));
        textViewMono.setPadding(dp(12), dp(12), dp(12), dp(12));
        textViewMono.setTextIsSelectable(true);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(textViewMono);
        new AlertDialog.Builder(this).setTitle("PREVIOUS CRASH DETECTED").setView(scrollView).setPositiveButton("COPY", new DialogInterface.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda12
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m81lambda$showCrashReportIfAny$0$netradiovoidbbsMainActivity(string, dialogInterface, i);
            }
        }).setNegativeButton("DISMISS", new DialogInterface.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda13
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m82lambda$showCrashReportIfAny$1$netradiovoidbbsMainActivity(dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: renamed from: lambda$showCrashReportIfAny$0$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m81lambda$showCrashReportIfAny$0$netradiovoidbbsMainActivity(String str, DialogInterface dialogInterface, int i) {
        ((ClipboardManager) getSystemService(ClipboardManager.class)).setPrimaryClip(ClipData.newPlainText("crash", str));
        this.prefs.edit().remove("last_crash").apply();
    }

    /* JADX INFO: renamed from: lambda$showCrashReportIfAny$1$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m82lambda$showCrashReportIfAny$1$netradiovoidbbsMainActivity(DialogInterface dialogInterface, int i) {
        this.prefs.edit().remove("last_crash").apply();
    }

    private void scheduleChatJob() {
        JobScheduler jobScheduler = (JobScheduler) getSystemService(JobScheduler.class);
        if (jobScheduler.getPendingJob(42) != null) {
            return;
        }
        jobScheduler.schedule(new JobInfo.Builder(42, new ComponentName(this, (Class<?>) ChatJobService.class)).setRequiredNetworkType(1).setPeriodic(900000L).setPersisted(true).build());
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!intent.getBooleanExtra("open_chat", false) || this.chatPanel == null || this.chatOpen) {
            return;
        }
        toggleChat();
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, (Class<?>) PlayerService.class), this.conn, 1);
        this.ui.post(this.ticker);
        this.ui.postDelayed(this.chatPoll, 4000L);
    }

    @Override // android.app.Activity
    protected void onStop() {
        PlayerService playerService;
        super.onStop();
        this.ui.removeCallbacks(this.ticker);
        this.ui.removeCallbacks(this.chatPoll);
        if (!this.bound || (playerService = this.svc) == null) {
            return;
        }
        playerService.savePosition();
        this.svc.setListener(null);
        unbindService(this.conn);
        this.bound = false;
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.chatOpen) {
            toggleChat();
        } else {
            super.onBackPressed();
        }
    }

    private void showBoot() {
        final MainActivity mainActivity = this;
        FrameLayout frameLayout = new FrameLayout(mainActivity);
        frameLayout.setBackgroundColor(-16777216);
        ScrollView scrollView = new ScrollView(mainActivity);
        final LinearLayout linearLayout = new LinearLayout(mainActivity);
        linearLayout.setOrientation(1);
        int iDp = mainActivity.dp(16);
        int i = iDp * 2;
        linearLayout.setPadding(iDp, i, iDp, i);
        scrollView.addView(linearLayout);
        frameLayout.addView(scrollView);
        final View view = new View(mainActivity);
        view.setBackgroundColor(mainActivity.th.fg);
        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(0, mainActivity.dp(3), 8388691);
        frameLayout.addView(view, layoutParams);
        frameLayout.addView(new ScanlineView(mainActivity));
        mainActivity.setContentView(frameLayout);
        final String[][] strArr = new String[25][];
        strArr[0] = new String[]{"╔══════════════════════════════════════╗", "a"};
        strArr[1] = new String[]{"║  RADIOVOID BBS - NATIVE ACCESS GRANT  ║", "a"};
        strArr[2] = new String[]{"╚══════════════════════════════════════╝", "a"};
        strArr[3] = new String[]{"", ""};
        strArr[4] = new String[]{"login: ********", ""};
        strArr[5] = new String[]{"AUTHENTICATING KEY HASH...", "w"};
        String[] strArr2 = new String[2];
        strArr2[0] = "ACCESS LEVEL: ".concat(Net.isAdmin() ? "SYSOP" : "LISTENER");
        strArr2[1] = "o";
        strArr[6] = strArr2;
        strArr[7] = new String[]{"", ""};
        strArr[8] = new String[]{"RADIOVOID BBS v2.2 - (C) 1993 MICKEY PERRY / VOID SYSTEMS", "b"};
        strArr[9] = new String[]{"", ""};
        strArr[10] = new String[]{"Initializing audio subsystem...........", ""};
        strArr[11] = new String[]{"  > MediaPlayer codec engine        [  OK  ]", "o"};
        strArr[12] = new String[]{"  > IBXM tracker replay (MOD/S3M/XM)[  OK  ]", "o"};
        strArr[13] = new String[]{"  > FFT spectrum analyzer (60FPS)   [  OK  ]", "o"};
        strArr[14] = new String[]{"Loading track database.................", ""};
        strArr[15] = new String[]{"  > Supabase connection             [  OK  ]", "o"};
        strArr[16] = new String[]{"  > Storage bucket mounted          [  OK  ]", "o"};
        strArr[17] = new String[]{"Verifying system integrity.............", ""};
        strArr[18] = new String[]{"  > CRT emulation module            [  OK  ]", "o"};
        strArr[19] = new String[]{"  > Scanline filter active          [  OK  ]", "o"};
        strArr[20] = new String[]{"  > Theme engine (7 presets)        [  OK  ]", "o"};
        strArr[21] = new String[]{"", ""};
        strArr[22] = new String[]{"All systems nominal.", "o"};
        strArr[23] = new String[]{"Welcome aboard. Enjoy the music.", "b"};
        strArr[24] = new String[]{"Entering terminal...", "w"};
        final boolean[] zArr = {false};
        final Runnable runnable = new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda25
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m78lambda$showBoot$3$netradiovoidbbsMainActivity(zArr);
            }
        };
        frameLayout.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda26
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                runnable.run();
            }
        });
        int i2 = 0;
        long j = 150;
        for (int i3 = 25; i2 < i3; i3 = 25) {
            final boolean[] zArr2 = zArr;
            final int i4 = i2;
            long j2 = j;
            final ScrollView scrollView2 = scrollView;
            mainActivity.ui.postDelayed(new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda27
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m80lambda$showBoot$7$netradiovoidbbsMainActivity(zArr2, strArr, i4, linearLayout, scrollView2, layoutParams, view);
                }
            }, j2);
            j = j2 + ((long) (((i4 % 3) * 50) + 60));
            i2 = i4 + 1;
        }
        final Runnable runnable2 = runnable;
        mainActivity.ui.postDelayed(new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda28
            @Override // java.lang.Runnable
            public final void run() {
                runnable2.run();
            }
        }, 350 + j);
    }

    /* JADX INFO: renamed from: lambda$showBoot$3$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m78lambda$showBoot$3$netradiovoidbbsMainActivity(boolean[] zArr) {
        if (zArr[0]) {
            return;
        }
        zArr[0] = true;
        buildMain();
        this.ui.post(new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda93
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m77lambda$showBoot$2$netradiovoidbbsMainActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$showBoot$2$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m77lambda$showBoot$2$netradiovoidbbsMainActivity() {
        GlitchView.play(this.root, this.th, this.mono, null, 420L, null);
    }

    /* JADX INFO: renamed from: lambda$showBoot$7$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m80lambda$showBoot$7$netradiovoidbbsMainActivity(boolean[] zArr, String[][] strArr, int i, LinearLayout linearLayout, ScrollView scrollView, FrameLayout.LayoutParams layoutParams, View view) {
        int color;
        if (zArr[0]) {
            return;
        }
        String[] strArr2 = strArr[i];
        final String str = strArr2[0];
        if (strArr2[1].equals("o")) {
            color = Color.parseColor("#33ff00");
        } else if (strArr[i][1].equals("w")) {
            color = Color.parseColor("#ffd866");
        } else if (strArr[i][1].equals("a")) {
            color = this.th.accent;
        } else {
            color = strArr[i][1].equals("b") ? this.th.fgBright : this.th.fg;
        }
        final TextView textViewMono = mono(str.isEmpty() ? " " : scramble(str), 17, color);
        linearLayout.addView(textViewMono);
        scrollView.fullScroll(130);
        if (!str.isEmpty()) {
            this.ui.postDelayed(new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m79lambda$showBoot$5$netradiovoidbbsMainActivity(textViewMono, str);
                }
            }, 40L);
            this.ui.postDelayed(new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    textViewMono.setText(str);
                }
            }, 90L);
        }
        layoutParams.width = (getResources().getDisplayMetrics().widthPixels * (i + 1)) / strArr.length;
        view.setLayoutParams(layoutParams);
    }

    /* JADX INFO: renamed from: lambda$showBoot$5$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m79lambda$showBoot$5$netradiovoidbbsMainActivity(TextView textView, String str) {
        textView.setText(scramble(str));
    }

    private String scramble(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt != ' ' && this.rnd.nextInt(3) != 0) {
                cCharAt = SCRAMBLE.charAt(this.rnd.nextInt(17));
            }
            sb.append(cCharAt);
        }
        return sb.toString();
    }

    private void buildMain() {
        this.themeAppliers.clear();
        this.root = new FrameLayout(this);
        LinearLayout linearLayout = new LinearLayout(this);
        this.mainCol = linearLayout;
        linearLayout.setOrientation(1);
        int iDp = dp(10);
        this.mainCol.setPadding(iDp, iDp, iDp, iDp);
        themed(this.mainCol, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda47
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m64lambda$buildMain$8$netradiovoidbbsMainActivity();
            }
        });
        this.root.addView(this.mainCol, new FrameLayout.LayoutParams(-1, -1));
        final TextView textViewMono = mono("≡≡≡ R A D I O V O I D ≡≡≡", 26, this.th.fgBright);
        textViewMono.setGravity(17);
        themed(textViewMono, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda58
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m65lambda$buildMain$9$netradiovoidbbsMainActivity(textViewMono);
            }
        });
        this.mainCol.addView(textViewMono);
        LinearLayout linearLayoutRow = row();
        TextView textViewMono2 = mono("NODE 7 · CONNECTING...", 15, this.th.fg);
        this.statusLine = textViewMono2;
        textViewMono2.setSingleLine(true);
        this.statusLine.setEllipsize(TextUtils.TruncateAt.END);
        themed(this.statusLine, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda69
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m22lambda$buildMain$10$netradiovoidbbsMainActivity();
            }
        });
        this.statusLine.setOnLongClickListener(new View.OnLongClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda80
            @Override // android.view.View.OnLongClickListener
            public final boolean onLongClick(View view) {
                return MainActivity.this.m23lambda$buildMain$11$netradiovoidbbsMainActivity(view);
            }
        });
        linearLayoutRow.addView(this.statusLine, lpw(0, 1.0f));
        TextView textViewBtn = btn("[SYS]");
        this.sysBtn = textViewBtn;
        themed(textViewBtn, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda85
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m24lambda$buildMain$12$netradiovoidbbsMainActivity();
            }
        });
        this.sysBtn.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda86
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m25lambda$buildMain$13$netradiovoidbbsMainActivity(view);
            }
        });
        linearLayoutRow.addView(this.sysBtn);
        final TextView textViewBtn2 = btn("[" + this.th.name + "]");
        themed(textViewBtn2, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda87
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m26lambda$buildMain$14$netradiovoidbbsMainActivity(textViewBtn2);
            }
        });
        textViewBtn2.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda88
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m27lambda$buildMain$15$netradiovoidbbsMainActivity(view);
            }
        });
        linearLayoutRow.addView(textViewBtn2);
        TextView textViewBtn3 = btn("[CHAT]");
        this.chatBtn = textViewBtn3;
        textViewBtn3.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda89
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m28lambda$buildMain$16$netradiovoidbbsMainActivity(view);
            }
        });
        linearLayoutRow.addView(this.chatBtn);
        this.mainCol.addView(linearLayoutRow, lpTop(4));
        LinearLayout linearLayoutBox = box();
        List<View> arrayList = new ArrayList<>();
        linearLayoutBox.addView(sectionHeader("▚▚ NOW PLAYING", "np", arrayList));
        TextView textViewMono3 = mono("[ NO TRACK LOADED ]", 22, this.th.fgBright);
        this.npTitle = textViewMono3;
        textViewMono3.setSingleLine(true);
        this.npTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        this.npTitle.setMarqueeRepeatLimit(-1);
        this.npTitle.setSelected(true);
        this.npTitle.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda90
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m29lambda$buildMain$17$netradiovoidbbsMainActivity(view);
            }
        });
        themed(this.npTitle, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda48
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m30lambda$buildMain$18$netradiovoidbbsMainActivity();
            }
        });
        linearLayoutBox.addView(this.npTitle);
        arrayList.add(this.npTitle);
        TextView textViewMono4 = mono("Load some tunes, dude...", 15, this.th.fgDim);
        this.npArtist = textViewMono4;
        textViewMono4.setSingleLine(true);
        this.npArtist.setEllipsize(TextUtils.TruncateAt.END);
        themed(this.npArtist, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda49
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m31lambda$buildMain$19$netradiovoidbbsMainActivity();
            }
        });
        linearLayoutBox.addView(this.npArtist);
        arrayList.add(this.npArtist);
        FrameLayout frameLayout = new FrameLayout(this);
        this.progBg = frameLayout;
        themed(frameLayout, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda50
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m32lambda$buildMain$20$netradiovoidbbsMainActivity();
            }
        });
        View view = new View(this);
        this.progFill = view;
        themed(view, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda51
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m33lambda$buildMain$21$netradiovoidbbsMainActivity();
            }
        });
        this.progBg.addView(this.progFill, new FrameLayout.LayoutParams(0, -1));
        View view2 = new View(this);
        this.progThumb = view2;
        themed(view2, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda52
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m34lambda$buildMain$22$netradiovoidbbsMainActivity();
            }
        });
        this.progBg.addView(this.progThumb, new FrameLayout.LayoutParams(dp(8), -1));
        this.progBg.setOnTouchListener(new View.OnTouchListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda53
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view3, MotionEvent motionEvent) {
                return MainActivity.this.m35lambda$buildMain$23$netradiovoidbbsMainActivity(view3, motionEvent);
            }
        });
        linearLayoutBox.addView(this.progBg, lpTopH(8, dp(18)));
        arrayList.add(this.progBg);
        LinearLayout linearLayoutRow2 = row();
        TextView textViewMono5 = mono("00:00", 14, this.th.fgDim);
        this.tCur = textViewMono5;
        themed(textViewMono5, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda54
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m36lambda$buildMain$24$netradiovoidbbsMainActivity();
            }
        });
        linearLayoutRow2.addView(this.tCur, lpw(0, 1.0f));
        TextView textViewMono6 = mono("00:00", 14, this.th.fgDim);
        this.tTot = textViewMono6;
        themed(textViewMono6, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda55
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m37lambda$buildMain$25$netradiovoidbbsMainActivity();
            }
        });
        linearLayoutRow2.addView(this.tTot);
        linearLayoutBox.addView(linearLayoutRow2);
        arrayList.add(linearLayoutRow2);
        this.audio = (AudioManager) getSystemService(AudioManager.class);
        LinearLayout linearLayoutRow3 = row();
        final TextView textViewMono7 = mono("VOL", 14, this.th.fgDim);
        themed(textViewMono7, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda56
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m38lambda$buildMain$26$netradiovoidbbsMainActivity(textViewMono7);
            }
        });
        linearLayoutRow3.addView(textViewMono7);
        FrameLayout frameLayout2 = new FrameLayout(this);
        this.volBg = frameLayout2;
        themed(frameLayout2, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda57
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m39lambda$buildMain$27$netradiovoidbbsMainActivity();
            }
        });
        View view3 = new View(this);
        this.volFill = view3;
        themed(view3, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda59
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m40lambda$buildMain$28$netradiovoidbbsMainActivity();
            }
        });
        this.volBg.addView(this.volFill, new FrameLayout.LayoutParams(0, -1));
        View view4 = new View(this);
        this.volThumb = view4;
        themed(view4, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda60
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m41lambda$buildMain$29$netradiovoidbbsMainActivity();
            }
        });
        this.volBg.addView(this.volThumb, new FrameLayout.LayoutParams(dp(8), -1));
        this.volBg.setOnTouchListener(new View.OnTouchListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda61
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view5, MotionEvent motionEvent) {
                return MainActivity.this.m42lambda$buildMain$30$netradiovoidbbsMainActivity(view5, motionEvent);
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, dp(16), 1.0f);
        layoutParams.leftMargin = dp(8);
        layoutParams.rightMargin = dp(8);
        linearLayoutRow3.addView(this.volBg, layoutParams);
        TextView textViewMono8 = mono("--%", 14, this.th.fgDim);
        this.volPct = textViewMono8;
        themed(textViewMono8, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda62
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m43lambda$buildMain$31$netradiovoidbbsMainActivity();
            }
        });
        linearLayoutRow3.addView(this.volPct);
        linearLayoutBox.addView(linearLayoutRow3, lpTop(6));
        arrayList.add(linearLayoutRow3);
        LinearLayout linearLayoutRow4 = row();
        TextView textViewBtn4 = btn("|<<");
        textViewBtn4.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda63
            @Override // android.view.View.OnClickListener
            public final void onClick(View view5) {
                MainActivity.this.m44lambda$buildMain$32$netradiovoidbbsMainActivity(view5);
            }
        });
        linearLayoutRow4.addView(textViewBtn4, lpw(0, 1.0f));
        TextView textViewBtn5 = btn("[ PLAY ]");
        this.bPlay = textViewBtn5;
        textViewBtn5.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda64
            @Override // android.view.View.OnClickListener
            public final void onClick(View view5) {
                MainActivity.this.m45lambda$buildMain$33$netradiovoidbbsMainActivity(view5);
            }
        });
        linearLayoutRow4.addView(this.bPlay, lpw(0, 1.6f));
        TextView textViewBtn6 = btn(">>|");
        textViewBtn6.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda65
            @Override // android.view.View.OnClickListener
            public final void onClick(View view5) {
                MainActivity.this.m46lambda$buildMain$34$netradiovoidbbsMainActivity(view5);
            }
        });
        linearLayoutRow4.addView(textViewBtn6, lpw(0, 1.0f));
        TextView textViewBtn7 = btn("SHUF");
        this.bShuf = textViewBtn7;
        textViewBtn7.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda66
            @Override // android.view.View.OnClickListener
            public final void onClick(View view5) {
                MainActivity.this.m47lambda$buildMain$35$netradiovoidbbsMainActivity(view5);
            }
        });
        linearLayoutRow4.addView(this.bShuf, lpw(0, 1.0f));
        TextView textViewBtn8 = btn("RPT");
        this.bRpt = textViewBtn8;
        textViewBtn8.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda67
            @Override // android.view.View.OnClickListener
            public final void onClick(View view5) {
                MainActivity.this.m48lambda$buildMain$36$netradiovoidbbsMainActivity(view5);
            }
        });
        linearLayoutRow4.addView(this.bRpt, lpw(0, 1.0f));
        linearLayoutBox.addView(linearLayoutRow4, lpTop(8));
        arrayList.add(linearLayoutRow4);
        applyCollapse("np", arrayList);
        this.mainCol.addView(linearLayoutBox, lpTop(8));
        LinearLayout linearLayoutBox2 = box();
        List<View> arrayList2 = new ArrayList<>();
        LinearLayout linearLayoutRow5 = row();
        linearLayoutRow5.addView(sectionHeader("AUDIO SPECTRUM ANALYZER", "viz", arrayList2), lpw(0, 1.0f));
        TextView textViewMono9 = mono("BARS", 13, this.th.accent);
        this.vizMode = textViewMono9;
        themed(textViewMono9, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda68
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m49lambda$buildMain$37$netradiovoidbbsMainActivity();
            }
        });
        linearLayoutRow5.addView(this.vizMode);
        linearLayoutBox2.addView(linearLayoutRow5);
        VizView vizView = new VizView(this);
        this.viz = vizView;
        vizView.setTheme(this.th);
        themed(this.viz, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda70
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m50lambda$buildMain$38$netradiovoidbbsMainActivity();
            }
        });
        this.viz.setOnModeChange(new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda71
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m51lambda$buildMain$39$netradiovoidbbsMainActivity();
            }
        });
        linearLayoutBox2.addView(this.viz, lpTopH(4, dp(96)));
        arrayList2.add(this.viz);
        applyCollapse("viz", arrayList2);
        this.mainCol.addView(linearLayoutBox2, lpTop(8));
        LinearLayout linearLayoutBox3 = box();
        List<View> arrayList3 = new ArrayList<>();
        LinearLayout linearLayoutRow6 = row();
        linearLayoutRow6.addView(sectionHeader("░ LIBRARY", "lib", arrayList3, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda72
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m52lambda$buildMain$40$netradiovoidbbsMainActivity();
            }
        }), lpw(0, 1.0f));
        TextView textViewMono10 = mono("[0]", 14, this.th.fgDim);
        this.trkCount = textViewMono10;
        themed(textViewMono10, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda73
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m53lambda$buildMain$41$netradiovoidbbsMainActivity();
            }
        });
        linearLayoutRow6.addView(this.trkCount);
        linearLayoutBox3.addView(linearLayoutRow6);
        LinearLayout linearLayoutRow7 = row();
        final EditText editText = new EditText(this);
        editText.setTypeface(this.mono);
        editText.setTextSize(16.0f);
        editText.setHint("SEARCH...");
        editText.setSingleLine(true);
        editText.setImeOptions(3);
        editText.setInputType(524289);
        int iDp2 = dp(6);
        editText.setPadding(iDp2, iDp2, iDp2, iDp2);
        themed(editText, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda74
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m54lambda$buildMain$42$netradiovoidbbsMainActivity(editText);
            }
        });
        editText.addTextChangedListener(new TextWatcher() { // from class: net.radiovoid.bbs.MainActivity.2
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable editable) {
                MainActivity.this.query = editable.toString().trim().toLowerCase(Locale.US);
                MainActivity.this.refilter();
            }
        });
        linearLayoutRow7.addView(editText, lpw(0, 1.0f));
        TextView textViewBtn9 = btn(viewLabel());
        this.viewBtn = textViewBtn9;
        textViewBtn9.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda75
            @Override // android.view.View.OnClickListener
            public final void onClick(View view5) {
                MainActivity.this.m55lambda$buildMain$43$netradiovoidbbsMainActivity(view5);
            }
        });
        this.viewBtn.setOnLongClickListener(new View.OnLongClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda76
            @Override // android.view.View.OnLongClickListener
            public final boolean onLongClick(View view5) {
                return MainActivity.this.m56lambda$buildMain$44$netradiovoidbbsMainActivity(view5);
            }
        });
        linearLayoutRow7.addView(this.viewBtn);
        TextView textViewBtn10 = btn("SORT:" + SORTS[this.sortIdx]);
        this.sortBtn = textViewBtn10;
        textViewBtn10.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda77
            @Override // android.view.View.OnClickListener
            public final void onClick(View view5) {
                MainActivity.this.m57lambda$buildMain$45$netradiovoidbbsMainActivity(view5);
            }
        });
        linearLayoutRow7.addView(this.sortBtn);
        TextView textViewBtn11 = btn("⚄");
        textViewBtn11.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda78
            @Override // android.view.View.OnClickListener
            public final void onClick(View view5) {
                MainActivity.this.m58lambda$buildMain$46$netradiovoidbbsMainActivity(view5);
            }
        });
        linearLayoutRow7.addView(textViewBtn11);
        linearLayoutBox3.addView(linearLayoutRow7, lpTop(6));
        arrayList3.add(linearLayoutRow7);
        ListView listView = new ListView(this);
        this.list = listView;
        listView.setDivider(null);
        TrackAdapter trackAdapter = new TrackAdapter();
        this.adapter = trackAdapter;
        this.list.setAdapter((ListAdapter) trackAdapter);
        this.list.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda79
            @Override // android.widget.AdapterView.OnItemClickListener
            public final void onItemClick(AdapterView adapterView, View view5, int i, long j) {
                MainActivity.this.m59lambda$buildMain$47$netradiovoidbbsMainActivity(adapterView, view5, i, j);
            }
        });
        this.list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda81
            @Override // android.widget.AdapterView.OnItemLongClickListener
            public final boolean onItemLongClick(AdapterView adapterView, View view5, int i, long j) {
                return MainActivity.this.m60lambda$buildMain$48$netradiovoidbbsMainActivity(adapterView, view5, i, j);
            }
        });
        themed(this.list, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda82
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m61lambda$buildMain$49$netradiovoidbbsMainActivity();
            }
        });
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        layoutParams2.topMargin = dp(6);
        linearLayoutBox3.addView(this.list, layoutParams2);
        arrayList3.add(this.list);
        applyCollapse("lib", arrayList3);
        this.mainCol.addView(linearLayoutBox3, lpTop(8));
        updateLibBoxParams(linearLayoutBox3);
        final TextView textViewMono11 = mono("▞ created by MICKEY PERRY ▚ · tap for credits", 12, this.th.fgDim);
        textViewMono11.setGravity(17);
        textViewMono11.setPadding(0, dp(2), 0, dp(2));
        themed(textViewMono11, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda83
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m62lambda$buildMain$50$netradiovoidbbsMainActivity(textViewMono11);
            }
        });
        textViewMono11.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda84
            @Override // android.view.View.OnClickListener
            public final void onClick(View view5) {
                MainActivity.this.m63lambda$buildMain$51$netradiovoidbbsMainActivity(view5);
            }
        });
        this.mainCol.addView(textViewMono11, lpTop(4));
        buildChat();
        this.root.addView(new ScanlineView(this));
        setContentView(this.root);
        applyTheme();
        loadLibrary();
        maybeAskBatteryExemption();
        if (!getIntent().getBooleanExtra("open_chat", false) || this.chatOpen) {
            return;
        }
        toggleChat();
    }

    /* JADX INFO: renamed from: lambda$buildMain$8$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m64lambda$buildMain$8$netradiovoidbbsMainActivity() {
        this.mainCol.setBackgroundColor(this.th.bg);
    }

    /* JADX INFO: renamed from: lambda$buildMain$9$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m65lambda$buildMain$9$netradiovoidbbsMainActivity(TextView textView) {
        textView.setTextColor(this.th.fgBright);
        textView.setShadowLayer(12.0f, 0.0f, 0.0f, this.th.fg);
    }

    /* JADX INFO: renamed from: lambda$buildMain$10$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m22lambda$buildMain$10$netradiovoidbbsMainActivity() {
        this.statusLine.setTextColor(this.flashMsg == null ? this.th.fg : this.th.accent);
    }

    /* JADX INFO: renamed from: lambda$buildMain$11$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ boolean m23lambda$buildMain$11$netradiovoidbbsMainActivity(View view) {
        showDebugLog();
        return true;
    }

    /* JADX INFO: renamed from: lambda$buildMain$12$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m24lambda$buildMain$12$netradiovoidbbsMainActivity() {
        this.sysBtn.setTextColor(Net.isAdmin() ? this.th.accent : this.th.fg);
    }

    /* JADX INFO: renamed from: lambda$buildMain$13$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m25lambda$buildMain$13$netradiovoidbbsMainActivity(View view) {
        sysopDialog();
    }

    /* JADX INFO: renamed from: lambda$buildMain$14$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m26lambda$buildMain$14$netradiovoidbbsMainActivity(TextView textView) {
        textView.setText("[" + this.th.name + "]");
    }

    /* JADX INFO: renamed from: lambda$buildMain$15$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m27lambda$buildMain$15$netradiovoidbbsMainActivity(View view) {
        cycleTheme();
    }

    /* JADX INFO: renamed from: lambda$buildMain$16$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m28lambda$buildMain$16$netradiovoidbbsMainActivity(View view) {
        toggleChat();
    }

    /* JADX INFO: renamed from: lambda$buildMain$17$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m29lambda$buildMain$17$netradiovoidbbsMainActivity(View view) {
        scrollToCurrent();
    }

    /* JADX INFO: renamed from: lambda$buildMain$18$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m30lambda$buildMain$18$netradiovoidbbsMainActivity() {
        this.npTitle.setTextColor(this.th.fgBright);
    }

    /* JADX INFO: renamed from: lambda$buildMain$19$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m31lambda$buildMain$19$netradiovoidbbsMainActivity() {
        this.npArtist.setTextColor(this.th.fgDim);
    }

    /* JADX INFO: renamed from: lambda$buildMain$20$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m32lambda$buildMain$20$netradiovoidbbsMainActivity() {
        this.progBg.setBackgroundColor(this.th.bg2);
    }

    /* JADX INFO: renamed from: lambda$buildMain$21$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m33lambda$buildMain$21$netradiovoidbbsMainActivity() {
        this.progFill.setBackgroundColor(this.th.fg);
    }

    /* JADX INFO: renamed from: lambda$buildMain$22$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m34lambda$buildMain$22$netradiovoidbbsMainActivity() {
        this.progThumb.setBackgroundColor(this.th.fgBright);
    }

    /* JADX INFO: renamed from: lambda$buildMain$23$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ boolean m35lambda$buildMain$23$netradiovoidbbsMainActivity(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() != 0 && motionEvent.getAction() != 2) {
            return false;
        }
        PlayerService playerService = this.svc;
        if (playerService == null || !playerService.hasTrack()) {
            return true;
        }
        float fMax = Math.max(0.0f, Math.min(1.0f, motionEvent.getX() / view.getWidth()));
        if (motionEvent.getAction() == 0) {
            view.performClick();
        }
        PlayerService playerService2 = this.svc;
        playerService2.seekTo((int) (fMax * playerService2.durationMs()));
        return true;
    }

    /* JADX INFO: renamed from: lambda$buildMain$24$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m36lambda$buildMain$24$netradiovoidbbsMainActivity() {
        this.tCur.setTextColor(this.th.fgDim);
    }

    /* JADX INFO: renamed from: lambda$buildMain$25$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m37lambda$buildMain$25$netradiovoidbbsMainActivity() {
        this.tTot.setTextColor(this.th.fgDim);
    }

    /* JADX INFO: renamed from: lambda$buildMain$26$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m38lambda$buildMain$26$netradiovoidbbsMainActivity(TextView textView) {
        textView.setTextColor(this.th.fgDim);
    }

    /* JADX INFO: renamed from: lambda$buildMain$27$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m39lambda$buildMain$27$netradiovoidbbsMainActivity() {
        this.volBg.setBackgroundColor(this.th.bg2);
    }

    /* JADX INFO: renamed from: lambda$buildMain$28$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m40lambda$buildMain$28$netradiovoidbbsMainActivity() {
        this.volFill.setBackgroundColor(this.th.accent);
    }

    /* JADX INFO: renamed from: lambda$buildMain$29$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m41lambda$buildMain$29$netradiovoidbbsMainActivity() {
        this.volThumb.setBackgroundColor(this.th.fgBright);
    }

    /* JADX INFO: renamed from: lambda$buildMain$30$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ boolean m42lambda$buildMain$30$netradiovoidbbsMainActivity(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() != 0 && motionEvent.getAction() != 2) {
            return false;
        }
        float fMax = Math.max(0.0f, Math.min(1.0f, motionEvent.getX() / view.getWidth()));
        if (motionEvent.getAction() == 0) {
            view.performClick();
        }
        this.audio.setStreamVolume(3, Math.round(fMax * this.audio.getStreamMaxVolume(3)), 0);
        updateVolumeUi();
        return true;
    }

    /* JADX INFO: renamed from: lambda$buildMain$31$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m43lambda$buildMain$31$netradiovoidbbsMainActivity() {
        this.volPct.setTextColor(this.th.fgDim);
    }

    /* JADX INFO: renamed from: lambda$buildMain$32$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m44lambda$buildMain$32$netradiovoidbbsMainActivity(View view) {
        PlayerService playerService = this.svc;
        if (playerService != null) {
            playerService.prev();
        }
    }

    /* JADX INFO: renamed from: lambda$buildMain$33$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m45lambda$buildMain$33$netradiovoidbbsMainActivity(View view) {
        PlayerService playerService = this.svc;
        if (playerService == null) {
            return;
        }
        if (!playerService.hasTrack() && !this.shown.isEmpty()) {
            this.svc.setQueueAndPlay(new ArrayList(this.shown), 0);
        } else {
            this.svc.playPause();
        }
    }

    /* JADX INFO: renamed from: lambda$buildMain$34$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m46lambda$buildMain$34$netradiovoidbbsMainActivity(View view) {
        PlayerService playerService = this.svc;
        if (playerService != null) {
            playerService.next(true);
        }
    }

    /* JADX INFO: renamed from: lambda$buildMain$35$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m47lambda$buildMain$35$netradiovoidbbsMainActivity(View view) {
        PlayerService playerService = this.svc;
        if (playerService == null) {
            return;
        }
        playerService.setShuffle(!playerService.shuffle);
        onState();
    }

    /* JADX INFO: renamed from: lambda$buildMain$36$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m48lambda$buildMain$36$netradiovoidbbsMainActivity(View view) {
        PlayerService playerService = this.svc;
        if (playerService == null) {
            return;
        }
        playerService.setRepeat(!playerService.repeat);
        onState();
    }

    /* JADX INFO: renamed from: lambda$buildMain$37$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m49lambda$buildMain$37$netradiovoidbbsMainActivity() {
        this.vizMode.setTextColor(this.th.accent);
    }

    /* JADX INFO: renamed from: lambda$buildMain$38$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m50lambda$buildMain$38$netradiovoidbbsMainActivity() {
        this.viz.setTheme(this.th);
    }

    /* JADX INFO: renamed from: lambda$buildMain$39$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m51lambda$buildMain$39$netradiovoidbbsMainActivity() {
        this.vizMode.setText(this.viz.modeName());
    }

    /* JADX INFO: renamed from: lambda$buildMain$40$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m52lambda$buildMain$40$netradiovoidbbsMainActivity() {
        updateLibBoxParams(null);
    }

    /* JADX INFO: renamed from: lambda$buildMain$41$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m53lambda$buildMain$41$netradiovoidbbsMainActivity() {
        this.trkCount.setTextColor(this.th.fgDim);
    }

    /* JADX INFO: renamed from: lambda$buildMain$42$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m54lambda$buildMain$42$netradiovoidbbsMainActivity(EditText editText) {
        editText.setTextColor(this.th.fg);
        editText.setHintTextColor(this.th.fgDim);
        editText.setBackground(stroke(this.th.fgDim, this.th.bg2));
    }

    /* JADX INFO: renamed from: lambda$buildMain$43$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m55lambda$buildMain$43$netradiovoidbbsMainActivity(View view) {
        List<String> listViewCycle = viewCycle();
        this.view = listViewCycle.get((listViewCycle.indexOf(this.view) + 1) % listViewCycle.size());
        this.prefs.edit().putString("view", this.view).apply();
        this.viewBtn.setText(viewLabel());
        refilter();
    }

    /* JADX INFO: renamed from: lambda$buildMain$44$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ boolean m56lambda$buildMain$44$netradiovoidbbsMainActivity(View view) {
        playlistTools();
        return true;
    }

    /* JADX INFO: renamed from: lambda$buildMain$45$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m57lambda$buildMain$45$netradiovoidbbsMainActivity(View view) {
        int i = this.sortIdx + 1;
        String[] strArr = SORTS;
        this.sortIdx = i % strArr.length;
        this.prefs.edit().putInt("sort", this.sortIdx).apply();
        this.sortBtn.setText("SORT:" + strArr[this.sortIdx]);
        refilter();
    }

    /* JADX INFO: renamed from: lambda$buildMain$46$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m58lambda$buildMain$46$netradiovoidbbsMainActivity(View view) {
        if (this.svc == null || this.shown.isEmpty()) {
            return;
        }
        this.svc.setQueueAndPlay(new ArrayList(this.shown), this.rnd.nextInt(this.shown.size()));
    }

    /* JADX INFO: renamed from: lambda$buildMain$47$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m59lambda$buildMain$47$netradiovoidbbsMainActivity(AdapterView adapterView, View view, int i, long j) {
        PlayerService playerService = this.svc;
        if (playerService != null) {
            playerService.setQueueAndPlay(new ArrayList(this.shown), i);
        }
    }

    /* JADX INFO: renamed from: lambda$buildMain$48$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ boolean m60lambda$buildMain$48$netradiovoidbbsMainActivity(AdapterView adapterView, View view, int i, long j) {
        trackMenu(this.shown.get(i));
        return true;
    }

    /* JADX INFO: renamed from: lambda$buildMain$49$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m61lambda$buildMain$49$netradiovoidbbsMainActivity() {
        this.list.setBackground(stroke(this.th.fgDim, this.th.bg2));
    }

    /* JADX INFO: renamed from: lambda$buildMain$50$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m62lambda$buildMain$50$netradiovoidbbsMainActivity(TextView textView) {
        textView.setTextColor(this.th.fgDim);
    }

    /* JADX INFO: renamed from: lambda$buildMain$51$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m63lambda$buildMain$51$netradiovoidbbsMainActivity(View view) {
        showAbout();
    }

    private void scrollToCurrent() {
        Track trackCurrent;
        PlayerService playerService = this.svc;
        if (playerService == null || this.list == null || (trackCurrent = playerService.current()) == null) {
            return;
        }
        for (int i = 0; i < this.shown.size(); i++) {
            if (this.shown.get(i) == trackCurrent || this.shown.get(i).id.equals(trackCurrent.id)) {
                this.list.smoothScrollToPosition(i);
                return;
            }
        }
    }

    private View sectionHeader(String str, String str2, List<View> list) {
        return sectionHeader(str, str2, list, null);
    }

    private View sectionHeader(final String str, final String str2, final List<View> list, final Runnable runnable) {
        final TextView textViewMono = mono("", 13, this.th.fgDim);
        themed(textViewMono, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m73lambda$sectionHeader$52$netradiovoidbbsMainActivity(textViewMono);
            }
        });
        final Runnable runnable2 = new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda20
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m74lambda$sectionHeader$53$netradiovoidbbsMainActivity(textViewMono, str2, str);
            }
        };
        runnable2.run();
        textViewMono.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda21
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m75lambda$sectionHeader$54$netradiovoidbbsMainActivity(str2, runnable2, list, runnable, view);
            }
        });
        textViewMono.setPadding(0, 0, dp(16), dp(2));
        return textViewMono;
    }

    /* JADX INFO: renamed from: lambda$sectionHeader$52$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m73lambda$sectionHeader$52$netradiovoidbbsMainActivity(TextView textView) {
        textView.setTextColor(this.th.fgDim);
    }

    /* JADX INFO: renamed from: lambda$sectionHeader$53$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m74lambda$sectionHeader$53$netradiovoidbbsMainActivity(TextView textView, String str, String str2) {
        StringBuilder sb = new StringBuilder();
        SharedPreferences sharedPreferences = this.prefs;
        StringBuilder sb2 = new StringBuilder("col_");
        sb2.append(str);
        sb.append(sharedPreferences.getBoolean(sb2.toString(), false) ? "[+] " : "[-] ");
        sb.append(str2);
        textView.setText(sb.toString());
    }

    /* JADX INFO: renamed from: lambda$sectionHeader$54$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m75lambda$sectionHeader$54$netradiovoidbbsMainActivity(String str, Runnable runnable, List list, Runnable runnable2, View view) {
        boolean z = !this.prefs.getBoolean("col_" + str, false);
        this.prefs.edit().putBoolean("col_" + str, z).apply();
        runnable.run();
        Iterator it = list.iterator();
        while (it.hasNext()) {
            ((View) it.next()).setVisibility(z ? 8 : 0);
        }
        if (runnable2 != null) {
            runnable2.run();
        }
    }

    private void updateLibBoxParams(LinearLayout linearLayout) {
        if (linearLayout != null) {
            this.libBox = linearLayout;
        }
        if (this.libBox == null) {
            return;
        }
        boolean z = this.prefs.getBoolean("col_lib", false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, z ? -2 : 0, z ? 0.0f : 1.0f);
        layoutParams.topMargin = dp(8);
        this.libBox.setLayoutParams(layoutParams);
    }

    private void applyCollapse(String str, List<View> list) {
        if (this.prefs.getBoolean("col_" + str, false)) {
            Iterator<View> it = list.iterator();
            while (it.hasNext()) {
                it.next().setVisibility(8);
            }
        }
    }

    private void sysopDialog() {
        if (Net.isAdmin()) {
            new AlertDialog.Builder(this).setTitle("SYSOP CONSOLE").setMessage("Connected as sysop@radiovoid.bbs\n\nLong-press a track or chat message to delete it.").setPositiveButton("LOG OUT", new DialogInterface.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda42
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.m84lambda$sysopDialog$56$netradiovoidbbsMainActivity(dialogInterface, i);
                }
            }).setNegativeButton("STAY", (DialogInterface.OnClickListener) null).show();
            return;
        }
        final EditText editText = new EditText(this);
        editText.setInputType(129);
        editText.setTypeface(this.mono);
        editText.setHint("PASSKEY");
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle("SYSOP LOGIN").setView(editText).setPositiveButton("JACK IN", (DialogInterface.OnClickListener) null).setNegativeButton("ABORT", (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda43
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                MainActivity.this.m88lambda$sysopDialog$60$netradiovoidbbsMainActivity(alertDialogCreate, editText, dialogInterface);
            }
        });
        alertDialogCreate.show();
    }

    /* JADX INFO: renamed from: lambda$sysopDialog$56$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m84lambda$sysopDialog$56$netradiovoidbbsMainActivity(DialogInterface dialogInterface, int i) {
        Net.logout(new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda91
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m83lambda$sysopDialog$55$netradiovoidbbsMainActivity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$sysopDialog$55$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m83lambda$sysopDialog$55$netradiovoidbbsMainActivity() {
        GlitchView.play(this.root, this.th, this.mono, "DISCONNECTED", 900L, null);
        flash("SYSOP SESSION CLOSED");
        applyTheme();
        updateAdminUi();
    }

    /* JADX INFO: renamed from: lambda$sysopDialog$60$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m88lambda$sysopDialog$60$netradiovoidbbsMainActivity(final AlertDialog alertDialog, final EditText editText, DialogInterface dialogInterface) {
        alertDialog.getButton(-1).setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m87lambda$sysopDialog$59$netradiovoidbbsMainActivity(editText, alertDialog, view);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$sysopDialog$59$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m87lambda$sysopDialog$59$netradiovoidbbsMainActivity(EditText editText, final AlertDialog alertDialog, View view) {
        String string = editText.getText().toString();
        if (string.isEmpty()) {
            return;
        }
        alertDialog.getButton(-1).setEnabled(false);
        editText.setEnabled(false);
        Net.login(string, new Consumer() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda24
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MainActivity.this.m86lambda$sysopDialog$58$netradiovoidbbsMainActivity(alertDialog, (Boolean) obj);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$sysopDialog$58$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m86lambda$sysopDialog$58$netradiovoidbbsMainActivity(AlertDialog alertDialog, Boolean bool) {
        alertDialog.dismiss();
        if (bool.booleanValue()) {
            GlitchView.play(this.root, this.th, this.mono, "ACCESS GRANTED", 1300L, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m85lambda$sysopDialog$57$netradiovoidbbsMainActivity();
                }
            });
        } else {
            GlitchView.play(this.root, this.th, this.mono, "ACCESS DENIED", 800L, null);
        }
    }

    /* JADX INFO: renamed from: lambda$sysopDialog$57$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m85lambda$sysopDialog$57$netradiovoidbbsMainActivity() {
        flash("WELCOME, SYSOP");
        applyTheme();
        updateAdminUi();
    }

    private void updateAdminUi() {
        TextView textView = this.chatClr;
        if (textView != null) {
            textView.setVisibility(Net.isAdmin() ? 0 : 8);
        }
    }

    private void confirmDeleteTrack(final Track track) {
        new AlertDialog.Builder(this).setTitle("DELETE TRACK").setMessage(track.artist + " - " + track.title + "\n\nThis removes the file and DB row. No undo.").setPositiveButton("DELETE", new DialogInterface.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda5
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m68lambda$confirmDeleteTrack$63$netradiovoidbbsMainActivity(track, dialogInterface, i);
            }
        }).setNegativeButton("KEEP", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: renamed from: lambda$confirmDeleteTrack$61$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m66lambda$confirmDeleteTrack$61$netradiovoidbbsMainActivity() {
        flash("TRACK DELETED");
        loadLibrary();
    }

    /* JADX INFO: renamed from: lambda$confirmDeleteTrack$63$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m68lambda$confirmDeleteTrack$63$netradiovoidbbsMainActivity(Track track, DialogInterface dialogInterface, int i) {
        Net.deleteTrack(track, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m66lambda$confirmDeleteTrack$61$netradiovoidbbsMainActivity();
            }
        }, new Consumer() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MainActivity.this.m67lambda$confirmDeleteTrack$62$netradiovoidbbsMainActivity((Exception) obj);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$confirmDeleteTrack$62$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m67lambda$confirmDeleteTrack$62$netradiovoidbbsMainActivity(Exception exc) {
        flash("DELETE FAILED: " + exc.getMessage());
    }

    private void buildChat() {
        FrameLayout frameLayout = new FrameLayout(this);
        this.chatPanel = frameLayout;
        frameLayout.setVisibility(8);
        this.root.addView(this.chatPanel, new FrameLayout.LayoutParams(-1, (int) (getResources().getDisplayMetrics().heightPixels * 0.55f), 80));
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        int iDp = dp(8);
        linearLayout.setPadding(iDp, iDp, iDp, iDp);
        themed(linearLayout, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda31
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m8lambda$buildChat$64$netradiovoidbbsMainActivity(linearLayout);
            }
        });
        this.chatPanel.addView(linearLayout, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout linearLayoutRow = row();
        final TextView textViewMono = mono("▒ CHAT", 16, this.th.accent);
        themed(textViewMono, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda33
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m9lambda$buildChat$65$netradiovoidbbsMainActivity(textViewMono);
            }
        });
        linearLayoutRow.addView(textViewMono, lpw(0, 1.0f));
        TextView textViewBtn = btn(this.prefs.getBoolean("chat_mute", false) ? "[MUTED]" : "[♪]");
        this.muteBtn = textViewBtn;
        textViewBtn.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda34
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m10lambda$buildChat$66$netradiovoidbbsMainActivity(view);
            }
        });
        linearLayoutRow.addView(this.muteBtn);
        final TextView textViewBtn2 = btn("as " + nick());
        textViewBtn2.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda35
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m11lambda$buildChat$67$netradiovoidbbsMainActivity(textViewBtn2, view);
            }
        });
        linearLayoutRow.addView(textViewBtn2);
        TextView textViewBtn3 = btn("CLR");
        this.chatClr = textViewBtn3;
        textViewBtn3.setVisibility(Net.isAdmin() ? 0 : 8);
        this.chatClr.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda36
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m14lambda$buildChat$70$netradiovoidbbsMainActivity(view);
            }
        });
        linearLayoutRow.addView(this.chatClr);
        TextView textViewBtn4 = btn("[X]");
        textViewBtn4.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda37
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m15lambda$buildChat$71$netradiovoidbbsMainActivity(view);
            }
        });
        linearLayoutRow.addView(textViewBtn4);
        linearLayout.addView(linearLayoutRow);
        ListView listView = new ListView(this);
        listView.setDivider(null);
        ChatAdapter chatAdapter = new ChatAdapter();
        this.chatAdapter = chatAdapter;
        listView.setAdapter((ListAdapter) chatAdapter);
        listView.setStackFromBottom(true);
        listView.setTranscriptMode(1);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda38
            @Override // android.widget.AdapterView.OnItemLongClickListener
            public final boolean onItemLongClick(AdapterView adapterView, View view, int i, long j) {
                return MainActivity.this.m18lambda$buildChat$74$netradiovoidbbsMainActivity(adapterView, view, i, j);
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override // android.widget.AdapterView.OnItemClickListener
            public final void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
                if (i >= 0 && i < MainActivity.this.chatMsgs.size()) {
                    MainActivity.this.chatMsgTapped(MainActivity.this.chatMsgs.get(i));
                }
            }
        });
        linearLayout.addView(listView, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        // reply bar (hidden until a message is tapped -> REPLY)
        LinearLayout replyBar = row();
        this.chatReplyBar = replyBar;
        TextView replyTxt = mono("", 13, this.th.fgDim);
        this.chatReplyTxt = replyTxt;
        replyTxt.setSingleLine(true);
        replyTxt.setEllipsize(TextUtils.TruncateAt.END);
        replyBar.addView(replyTxt, lpw(0, 1.0f));
        TextView replyCancel = btn("[×]");
        replyCancel.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.cancelReply();
            }
        });
        replyBar.addView(replyCancel);
        replyBar.setVisibility(8);
        linearLayout.addView(replyBar, lpTop(4));

        LinearLayout linearLayoutRow2 = row();
        TextView npBtn = btn("[♫]");
        npBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.npShare();
            }
        });
        linearLayoutRow2.addView(npBtn);
        final EditText editText = new EditText(this);
        editText.setTypeface(this.mono);
        editText.setTextSize(16.0f);
        editText.setHint("type a message…");
        editText.setSingleLine(true);
        editText.setImeOptions(4);
        int iDp2 = dp(6);
        editText.setPadding(iDp2, iDp2, iDp2, iDp2);
        themed(editText, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda39
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m19lambda$buildChat$75$netradiovoidbbsMainActivity(editText);
            }
        });
        linearLayoutRow2.addView(editText, lpw(0, 1.0f));
        TextView textViewBtn5 = btn("SEND");
        final Runnable runnable = new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda40
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m21lambda$buildChat$77$netradiovoidbbsMainActivity(editText);
            }
        };
        textViewBtn5.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda41
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                runnable.run();
            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda32
            @Override // android.widget.TextView.OnEditorActionListener
            public final boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                return MainActivity.lambda$buildChat$79(runnable, textView, i, keyEvent);
            }
        });
        linearLayoutRow2.addView(textViewBtn5);
        linearLayout.addView(linearLayoutRow2, lpTop(6));
    }

    /* JADX INFO: renamed from: lambda$buildChat$64$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m8lambda$buildChat$64$netradiovoidbbsMainActivity(LinearLayout linearLayout) {
        linearLayout.setBackground(stroke(this.th.fg, this.th.bg));
    }

    /* JADX INFO: renamed from: lambda$buildChat$65$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m9lambda$buildChat$65$netradiovoidbbsMainActivity(TextView textView) {
        textView.setTextColor(this.th.accent);
    }

    /* JADX INFO: renamed from: lambda$buildChat$66$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m10lambda$buildChat$66$netradiovoidbbsMainActivity(View view) {
        boolean z = !this.prefs.getBoolean("chat_mute", false);
        this.prefs.edit().putBoolean("chat_mute", z).apply();
        this.muteBtn.setText(z ? "[MUTED]" : "[♪]");
    }

    /* JADX INFO: renamed from: lambda$buildChat$67$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m11lambda$buildChat$67$netradiovoidbbsMainActivity(TextView textView, View view) {
        askNick(textView);
    }

    /* JADX INFO: renamed from: lambda$buildChat$70$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m14lambda$buildChat$70$netradiovoidbbsMainActivity(View view) {
        new AlertDialog.Builder(this).setTitle("CLEAR ALL CHAT?").setPositiveButton("WIPE", new DialogInterface.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda22
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m13lambda$buildChat$69$netradiovoidbbsMainActivity(dialogInterface, i);
            }
        }).setNegativeButton("KEEP", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: renamed from: lambda$buildChat$69$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m13lambda$buildChat$69$netradiovoidbbsMainActivity(DialogInterface dialogInterface, int i) {
        Net.clearChat(new MainActivity$$ExternalSyntheticLambda10(this), new Consumer() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda11
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MainActivity.this.m12lambda$buildChat$68$netradiovoidbbsMainActivity((Exception) obj);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$buildChat$68$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m12lambda$buildChat$68$netradiovoidbbsMainActivity(Exception exc) {
        flash("CLEAR FAILED");
    }

    /* JADX INFO: renamed from: lambda$buildChat$71$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m15lambda$buildChat$71$netradiovoidbbsMainActivity(View view) {
        toggleChat();
    }

    /* JADX INFO: renamed from: lambda$buildChat$74$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ boolean m18lambda$buildChat$74$netradiovoidbbsMainActivity(AdapterView adapterView, View view, int i, long j) {
        if (!Net.isAdmin()) {
            return false;
        }
        final JSONObject jSONObject = this.chatMsgs.get(i);
        new AlertDialog.Builder(this).setTitle("DELETE MESSAGE").setMessage("<" + jSONObject.optString("nick") + "> " + jSONObject.optString("body")).setPositiveButton("DELETE", new DialogInterface.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda45
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                MainActivity.this.m17lambda$buildChat$73$netradiovoidbbsMainActivity(jSONObject, dialogInterface, i2);
            }
        }).setNegativeButton("KEEP", (DialogInterface.OnClickListener) null).show();
        return true;
    }

    /* JADX INFO: renamed from: lambda$buildChat$72$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m16lambda$buildChat$72$netradiovoidbbsMainActivity(Exception exc) {
        flash("DELETE FAILED");
    }

    /* JADX INFO: renamed from: lambda$buildChat$73$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m17lambda$buildChat$73$netradiovoidbbsMainActivity(JSONObject jSONObject, DialogInterface dialogInterface, int i) {
        Net.deleteMessage(jSONObject.optLong("id"), new MainActivity$$ExternalSyntheticLambda10(this), new Consumer() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda14
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MainActivity.this.m16lambda$buildChat$72$netradiovoidbbsMainActivity((Exception) obj);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$buildChat$75$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m19lambda$buildChat$75$netradiovoidbbsMainActivity(EditText editText) {
        editText.setTextColor(this.th.fg);
        editText.setHintTextColor(this.th.fgDim);
        editText.setBackground(stroke(this.th.fgDim, this.th.bg2));
    }

    /* JADX INFO: renamed from: lambda$buildChat$77$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m21lambda$buildChat$77$netradiovoidbbsMainActivity(EditText editText) {
        String strTrim = editText.getText().toString().trim();
        if (strTrim.isEmpty()) {
            return;
        }
        JSONObject replyTo = this.chatReplyTo;
        if (replyTo != null) {
            strTrim = ">>" + replyTo.optLong("id") + " " + strTrim;
            cancelReply();
        }
        if (strTrim.length() > 280) {
            strTrim = strTrim.substring(0, 280);
        }
        editText.setText("");
        Net.sendMessage(nick(), strTrim, new MainActivity$$ExternalSyntheticLambda10(this), new Consumer() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda97
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MainActivity.this.m20lambda$buildChat$76$netradiovoidbbsMainActivity((Exception) obj);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$buildChat$76$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m20lambda$buildChat$76$netradiovoidbbsMainActivity(Exception exc) {
        flash("CHAT SEND FAILED");
    }

    static /* synthetic */ boolean lambda$buildChat$79(Runnable runnable, TextView textView, int i, KeyEvent keyEvent) {
        runnable.run();
        return true;
    }

    private void toggleChat() {
        boolean z = !this.chatOpen;
        this.chatOpen = z;
        this.chatPanel.setVisibility(z ? 0 : 8);
        if (this.chatOpen) {
            loadChat();
        }
    }

    /* JADX INFO: renamed from: net.radiovoid.bbs.MainActivity$3, reason: invalid class name */
    class AnonymousClass3 implements Runnable {
        /* synthetic */ void lambda$run$1(Exception exc) {
        }

        AnonymousClass3() {
        }

        @Override // java.lang.Runnable
        public void run() {
            Net.fetchMessages(new Consumer() { // from class: net.radiovoid.bbs.MainActivity$3$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AnonymousClass3.this.m90lambda$run$0$netradiovoidbbsMainActivity$3((JSONArray) obj);
                }
            }, new Consumer() { // from class: net.radiovoid.bbs.MainActivity$3$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AnonymousClass3.this.lambda$run$1((Exception) obj);
                }
            });
            MainActivity.this.ui.postDelayed(this, MainActivity.this.chatOpen ? 12000L : 25000L);
        }

        /* JADX INFO: renamed from: lambda$run$0$net-radiovoid-bbs-MainActivity$3, reason: not valid java name */
        /* synthetic */ void m90lambda$run$0$netradiovoidbbsMainActivity$3(JSONArray jSONArray) {
            MainActivity.this.chatMsgs.clear();
            long j = 0;
            boolean z = false;
            for (int length = jSONArray.length() - 1; length >= 0; length--) {
                JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(length);
                if (jSONObjectOptJSONObject != null) {
                    MainActivity.this.chatMsgs.add(jSONObjectOptJSONObject);
                    long jOptLong = jSONObjectOptJSONObject.optLong("id");
                    if (jOptLong > j) {
                        j = jOptLong;
                    }
                    if (jOptLong > MainActivity.this.lastSeenMsg && !jSONObjectOptJSONObject.optString("nick").equals(MainActivity.this.nick())) {
                        z = true;
                    }
                }
            }
            if (MainActivity.this.chatAdapter != null) {
                MainActivity.this.chatAdapter.notifyDataSetChanged();
            }
            if (MainActivity.this.chatOpen) {
                MainActivity.this.markSeen(j);
            } else if (z) {
                if (MainActivity.this.chatBtn != null) {
                    MainActivity.this.chatBtn.setText("[CHAT*]");
                }
                MainActivity.this.beep();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void markSeen(long j) {
        if (j > this.lastSeenMsg) {
            this.lastSeenMsg = j;
            this.prefs.edit().putLong("chat_seen", j).apply();
        }
        TextView textView = this.chatBtn;
        if (textView != null) {
            textView.setText("[CHAT]");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void beep() {
        if (this.prefs.getBoolean("chat_mute", false)) {
            return;
        }
        try {
            MediaPlayer mediaPlayerCreate = MediaPlayer.create(this, R.raw.chat_beep);
            if (mediaPlayerCreate != null) {
                mediaPlayerCreate.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda9
                    @Override // android.media.MediaPlayer.OnCompletionListener
                    public final void onCompletion(MediaPlayer mediaPlayer) {
                        mediaPlayer.release();
                    }
                });
                mediaPlayerCreate.start();
            }
        } catch (Exception unused) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadChat() {
        this.ui.removeCallbacks(this.chatPoll);
        this.ui.post(this.chatPoll);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String nick() {
        return this.prefs.getString("nick", "anon");
    }

    private void askNick(final TextView textView) {
        final EditText editText = new EditText(this);
        editText.setText(nick());
        editText.setSingleLine(true);
        new AlertDialog.Builder(this).setTitle("HANDLE").setView(editText).setPositiveButton("OK", new DialogInterface.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda17
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m5lambda$askNick$80$netradiovoidbbsMainActivity(editText, textView, dialogInterface, i);
            }
        }).setNegativeButton("CANCEL", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: renamed from: lambda$askNick$80$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m5lambda$askNick$80$netradiovoidbbsMainActivity(EditText editText, TextView textView, DialogInterface dialogInterface, int i) {
        String strTrim = editText.getText().toString().trim();
        if (strTrim.isEmpty()) {
            strTrim = "anon";
        }
        if (strTrim.length() > 24) {
            strTrim = strTrim.substring(0, 24);
        }
        this.prefs.edit().putString("nick", strTrim).apply();
        textView.setText("as " + strTrim);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadLibrary() {
        this.statusLine.setText("NODE 7 · CONNECTING...");
        Net.fetchTracks(new Consumer() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda29
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MainActivity.this.m69lambda$loadLibrary$81$netradiovoidbbsMainActivity((List) obj);
            }
        }, new Consumer() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda30
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MainActivity.this.m70lambda$loadLibrary$82$netradiovoidbbsMainActivity((Exception) obj);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$loadLibrary$81$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m69lambda$loadLibrary$81$netradiovoidbbsMainActivity(List list) {
        this.allTracks.clear();
        this.allTracks.addAll(list);
        refilter();
        this.statusLine.setText(statusText());
        maybeRestoreSession();
    }

    /* JADX INFO: renamed from: lambda$loadLibrary$82$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m70lambda$loadLibrary$82$netradiovoidbbsMainActivity(Exception exc) {
        this.statusLine.setText("NODE 7 · OFFLINE · RETRYING");
        this.ui.postDelayed(new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.loadLibrary();
            }
        }, 5000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeRestoreSession() {
        String strSavedTrackId;
        PlayerService playerService = this.svc;
        if (playerService == null || playerService.hasTrack() || this.allTracks.isEmpty() || (strSavedTrackId = this.svc.savedTrackId()) == null || strSavedTrackId.isEmpty()) {
            return;
        }
        ArrayList arrayList = new ArrayList(this.shown.isEmpty() ? this.allTracks : this.shown);
        for (int i = 0; i < arrayList.size(); i++) {
            if (((Track) arrayList.get(i)).id.equals(strSavedTrackId)) {
                if (((Track) arrayList.get(i)).isPlayable()) {
                    PlayerService playerService2 = this.svc;
                    playerService2.setQueueAndResume(arrayList, i, playerService2.savedPos());
                    flash("RESUMED: " + ((Track) arrayList.get(i)).title);
                    return;
                }
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String statusText() {
        StringBuilder sb = new StringBuilder("NODE 7 · ");
        sb.append(Net.isAdmin() ? "SYSOP" : "ONLINE");
        sb.append(" · ");
        sb.append(this.allTracks.size());
        sb.append(" TRKS");
        return sb.toString();
    }

    private String viewLabel() {
        if (!this.view.startsWith("PL:")) {
            return this.view;
        }
        return "≡" + this.view.substring(3);
    }

    private List<String> viewCycle() {
        ArrayList arrayList = new ArrayList();
        arrayList.add("ALL");
        arrayList.add("MUSIC");
        arrayList.add("MODS");
        Iterator<String> itKeys = playlists().keys();
        ArrayList arrayList2 = new ArrayList();
        while (itKeys.hasNext()) {
            arrayList2.add(itKeys.next());
        }
        Collections.sort(arrayList2);
        Iterator it = arrayList2.iterator();
        while (it.hasNext()) {
            arrayList.add("PL:" + ((String) it.next()));
        }
        if (!arrayList.contains(this.view)) {
            this.view = "ALL";
        }
        return arrayList;
    }

    private JSONObject playlists() {
        try {
            return new JSONObject(this.prefs.getString("playlists", "{}"));
        } catch (Exception unused) {
            return new JSONObject();
        }
    }

    private void savePlaylists(JSONObject jSONObject) {
        this.prefs.edit().putString("playlists", jSONObject.toString()).apply();
    }

    private Set<String> playlistIds(String str) {
        HashSet hashSet = new HashSet();
        JSONArray jSONArrayOptJSONArray = playlists().optJSONArray(str);
        if (jSONArrayOptJSONArray != null) {
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                hashSet.add(jSONArrayOptJSONArray.optString(i));
            }
        }
        return hashSet;
    }

    private void addToPlaylist(String str, Track track) {
        try {
            JSONObject jSONObjectPlaylists = playlists();
            JSONArray jSONArrayOptJSONArray = jSONObjectPlaylists.optJSONArray(str);
            if (jSONArrayOptJSONArray == null) {
                jSONArrayOptJSONArray = new JSONArray();
            }
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                if (track.id.equals(jSONArrayOptJSONArray.optString(i))) {
                    flash("ALREADY IN " + str);
                    return;
                }
            }
            jSONArrayOptJSONArray.put(track.id);
            jSONObjectPlaylists.put(str, jSONArrayOptJSONArray);
            savePlaylists(jSONObjectPlaylists);
            flash("ADDED TO " + str);
        } catch (Exception unused) {
            flash("PLAYLIST ERROR");
        }
    }

    private void removeFromPlaylist(String str, Track track) {
        try {
            JSONObject jSONObjectPlaylists = playlists();
            JSONArray jSONArrayOptJSONArray = jSONObjectPlaylists.optJSONArray(str);
            if (jSONArrayOptJSONArray == null) {
                return;
            }
            JSONArray jSONArray = new JSONArray();
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                if (!track.id.equals(jSONArrayOptJSONArray.optString(i))) {
                    jSONArray.put(jSONArrayOptJSONArray.optString(i));
                }
            }
            if (jSONArray.length() == 0) {
                jSONObjectPlaylists.remove(str);
            } else {
                jSONObjectPlaylists.put(str, jSONArray);
            }
            savePlaylists(jSONObjectPlaylists);
        } catch (Exception unused) {
        }
    }

    private void trackMenu(final Track track) {
        final ArrayList arrayList = new ArrayList();
        arrayList.add("ADD TO PLAYLIST…");
        arrayList.add("SHARE TRACK");
        if (this.view.startsWith("PL:")) {
            arrayList.add("REMOVE FROM " + this.view.substring(3));
        }
        if (Net.isAdmin()) {
            arrayList.add("DELETE FROM SERVER");
        }
        new AlertDialog.Builder(this).setTitle(track.artist + " - " + track.title).setItems((CharSequence[]) arrayList.toArray(new String[0]), new DialogInterface.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda92
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m89lambda$trackMenu$83$netradiovoidbbsMainActivity(arrayList, track, dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: renamed from: lambda$trackMenu$83$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m89lambda$trackMenu$83$netradiovoidbbsMainActivity(List list, Track track, DialogInterface dialogInterface, int i) {
        String str = (String) list.get(i);
        if (str.startsWith("ADD")) {
            addToPlaylistDialog(track);
            return;
        }
        if (str.startsWith("SHARE")) {
            shareTrack(track);
            return;
        }
        if (str.startsWith("REMOVE")) {
            removeFromPlaylist(this.view.substring(3), track);
            viewCycle();
            this.viewBtn.setText(viewLabel());
            refilter();
            return;
        }
        confirmDeleteTrack(track);
    }

    private void shareTrack(Track track) {
        String str;
        if (track.isModule()) {
            str = "https://mickeyperry.github.io/radiovoid-bbs/?play=" + track.id;
        } else {
            str = track.url;
        }
        startActivity(Intent.createChooser(new Intent("android.intent.action.SEND").setType("text/plain").putExtra("android.intent.extra.TEXT", "🎵 " + track.artist + " - " + track.title + "\nlisten: " + str + "\nfrom RADIOVOID BBS → https://mickeyperry.github.io/radiovoid-bbs/"), "SHARE TRACK"));
    }

    private void sharePlaylist(String str) {
        StringBuilder sb = new StringBuilder("≡ " + str + " — a RadioVoid BBS playlist\n\n");
        int i = 1;
        for (String str2 : playlistIds(str)) {
            for (Track track : this.allTracks) {
                if (track.id.equals(str2)) {
                    sb.append(i);
                    sb.append(". ");
                    sb.append(track.artist);
                    sb.append(" - ");
                    sb.append(track.title);
                    sb.append("\n   ");
                    sb.append(track.url);
                    sb.append("\n");
                    i++;
                    break;
                }
            }
        }
        sb.append("\nfrom RADIOVOID BBS → https://mickeyperry.github.io/radiovoid-bbs/");
        startActivity(Intent.createChooser(new Intent("android.intent.action.SEND").setType("text/plain").putExtra("android.intent.extra.TEXT", sb.toString()), "SHARE PLAYLIST"));
    }

    private void playlistTools() {
        final ArrayList arrayList = new ArrayList();
        if (this.view.startsWith("PL:")) {
            arrayList.add("SHARE " + this.view.substring(3));
        }
        arrayList.add("BACKUP PLAYLISTS → DOWNLOADS");
        arrayList.add("IMPORT FROM CLIPBOARD");
        new AlertDialog.Builder(this).setTitle("PLAYLIST TOOLS").setItems((CharSequence[]) arrayList.toArray(new String[0]), new DialogInterface.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda3
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m72lambda$playlistTools$84$netradiovoidbbsMainActivity(arrayList, dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: renamed from: lambda$playlistTools$84$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m72lambda$playlistTools$84$netradiovoidbbsMainActivity(List list, DialogInterface dialogInterface, int i) {
        String str = (String) list.get(i);
        if (str.startsWith("SHARE")) {
            sharePlaylist(this.view.substring(3));
        } else if (str.startsWith("BACKUP")) {
            exportPlaylists();
        } else {
            importPlaylists();
        }
    }

    private void exportPlaylists() {
        try {
            String string = playlists().toString(2);
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("_display_name", "radiovoid-playlists.json");
                contentValues.put("mime_type", "application/json");
                OutputStream outputStreamOpenOutputStream = getContentResolver().openOutputStream(getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues));
                try {
                    outputStreamOpenOutputStream.write(string.getBytes(StandardCharsets.UTF_8));
                    if (outputStreamOpenOutputStream != null) {
                        outputStreamOpenOutputStream.close();
                    }
                    flash("SAVED TO DOWNLOADS");
                    return;
                } catch (Throwable th) {
                    if (outputStreamOpenOutputStream != null) {
                        try {
                            outputStreamOpenOutputStream.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            }
            startActivity(Intent.createChooser(new Intent("android.intent.action.SEND").setType("application/json").putExtra("android.intent.extra.TEXT", string), "EXPORT PLAYLISTS"));
        } catch (Exception unused) {
            flash("EXPORT FAILED");
        }
    }

    private void importPlaylists() {
        try {
            int i = 0;
            JSONObject jSONObject = new JSONObject(((ClipboardManager) getSystemService(ClipboardManager.class)).getPrimaryClip().getItemAt(0).getText().toString());
            if (jSONObject.has("playlists")) {
                jSONObject = jSONObject.getJSONObject("playlists");
            }
            JSONObject jSONObjectPlaylists = playlists();
            Iterator<String> itKeys = jSONObject.keys();
            while (itKeys.hasNext()) {
                String next = itKeys.next();
                JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray(next);
                if (jSONArrayOptJSONArray != null) {
                    jSONObjectPlaylists.put(next.toUpperCase(Locale.US), jSONArrayOptJSONArray);
                    i++;
                }
            }
            savePlaylists(jSONObjectPlaylists);
            refilter();
            StringBuilder sb = new StringBuilder();
            sb.append("IMPORTED ");
            sb.append(i);
            sb.append(" PLAYLIST");
            sb.append(i == 1 ? "" : "S");
            flash(sb.toString());
        } catch (Exception unused) {
            flash("CLIPBOARD HAS NO PLAYLIST JSON");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addToPlaylistDialog(final Track track) {
        JSONObject jSONObjectPlaylists = playlists();
        final ArrayList arrayList = new ArrayList();
        Iterator<String> itKeys = jSONObjectPlaylists.keys();
        while (itKeys.hasNext()) {
            arrayList.add(itKeys.next());
        }
        Collections.sort(arrayList);
        arrayList.add("+ NEW PLAYLIST…");
        new AlertDialog.Builder(this).setTitle("ADD TO PLAYLIST").setItems((CharSequence[]) arrayList.toArray(new String[0]), new DialogInterface.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda16
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m4lambda$addToPlaylistDialog$86$netradiovoidbbsMainActivity(arrayList, track, dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: renamed from: lambda$addToPlaylistDialog$86$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m4lambda$addToPlaylistDialog$86$netradiovoidbbsMainActivity(List list, final Track track, DialogInterface dialogInterface, int i) {
        if (i == list.size() - 1) {
            final EditText editText = new EditText(this);
            editText.setHint("playlist name");
            editText.setSingleLine(true);
            new AlertDialog.Builder(this).setTitle("NEW PLAYLIST").setView(editText).setPositiveButton("CREATE", new DialogInterface.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda44
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface2, int i2) {
                    MainActivity.this.m3lambda$addToPlaylistDialog$85$netradiovoidbbsMainActivity(editText, track, dialogInterface2, i2);
                }
            }).setNegativeButton("CANCEL", (DialogInterface.OnClickListener) null).show();
            return;
        }
        addToPlaylist((String) list.get(i), track);
    }

    /* JADX INFO: renamed from: lambda$addToPlaylistDialog$85$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m3lambda$addToPlaylistDialog$85$netradiovoidbbsMainActivity(EditText editText, Track track, DialogInterface dialogInterface, int i) {
        String upperCase = editText.getText().toString().trim().toUpperCase(Locale.US);
        if (upperCase.isEmpty() || upperCase.equals("ALL") || upperCase.equals("MUSIC") || upperCase.equals("MODS")) {
            return;
        }
        if (upperCase.length() > 16) {
            upperCase = upperCase.substring(0, 16);
        }
        addToPlaylist(upperCase, track);
    }

    private boolean inView(Track track) {
        if (this.view.equals("MUSIC")) {
            return !track.isModule();
        }
        if (this.view.equals("MODS")) {
            // Honorary mods: titles shown under MODS regardless of file format.
            return track.isModule() || "ariel must die".equalsIgnoreCase(track.title);
        }
        if (this.view.startsWith("PL:")) {
            return playlistIds(this.view.substring(3)).contains(track.id);
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refilter() {
        this.shown.clear();
        for (Track track : this.allTracks) {
            if (inView(track) && (this.query.isEmpty() || track.title.toLowerCase(Locale.US).contains(this.query) || track.artist.toLowerCase(Locale.US).contains(this.query))) {
                this.shown.add(track);
            }
        }
        String str = SORTS[this.sortIdx];
        str.hashCode();
        switch (str) {
            case "RANDOM":
                Collections.shuffle(this.shown);
                break;
            case "YEAR":
                Collections.sort(this.shown, new Comparator() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda96
                    @Override // java.util.Comparator
                    public final int compare(Object obj, Object obj2) {
                        return Integer.compare(((Track) obj2).year, ((Track) obj).year);
                    }
                });
                break;
            case "TITLE":
                Collections.sort(this.shown, new Comparator() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda94
                    @Override // java.util.Comparator
                    public final int compare(Object obj, Object obj2) {
                        return ((Track) obj).title.compareToIgnoreCase(((Track) obj2).title);
                    }
                });
                break;
            case "ARTIST":
                Collections.sort(this.shown, new Comparator() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda95
                    @Override // java.util.Comparator
                    public final int compare(Object obj, Object obj2) {
                        return ((Track) obj).artist.compareToIgnoreCase(((Track) obj2).artist);
                    }
                });
                break;
        }
        this.trkCount.setText("[" + this.shown.size() + "]");
        this.adapter.notifyDataSetChanged();
    }

    /* JADX INFO: Access modifiers changed from: private */
    class TrackAdapter extends BaseAdapter {
        private TrackAdapter() {
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return MainActivity.this.shown.size();
        }

        @Override // android.widget.Adapter
        public Object getItem(int i) {
            return MainActivity.this.shown.get(i);
        }

        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return ((Track) MainActivity.this.shown.get(i)).id.hashCode();
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            LinearLayout linearLayout;
            TextView textViewMono;
            TextView textView;
            TextView textView2;
            int i2;
            String str = "";
            if ((view instanceof LinearLayout) && "trk".equals(view.getTag())) {
                linearLayout = (LinearLayout) view;
                LinearLayout linearLayout2 = (LinearLayout) linearLayout.getChildAt(0);
                textViewMono = (TextView) linearLayout2.getChildAt(0);
                textView2 = (TextView) linearLayout2.getChildAt(1);
                textView = (TextView) linearLayout.getChildAt(1);
            } else {
                linearLayout = new LinearLayout(MainActivity.this);
                linearLayout.setTag("trk");
                linearLayout.setOrientation(0);
                linearLayout.setGravity(16);
                linearLayout.setPadding(MainActivity.this.dp(8), MainActivity.this.dp(4), 0, MainActivity.this.dp(4));
                LinearLayout linearLayout3 = new LinearLayout(MainActivity.this);
                linearLayout3.setOrientation(1);
                MainActivity mainActivity = MainActivity.this;
                textViewMono = mainActivity.mono("", 18, mainActivity.th.fg);
                MainActivity mainActivity2 = MainActivity.this;
                TextView textViewMono2 = mainActivity2.mono("", 13, mainActivity2.th.fgDim);
                textViewMono.setSingleLine(true);
                textViewMono.setEllipsize(TextUtils.TruncateAt.END);
                linearLayout3.addView(textViewMono);
                linearLayout3.addView(textViewMono2);
                linearLayout.addView(linearLayout3, new LinearLayout.LayoutParams(0, -2, 1.0f));
                MainActivity mainActivity3 = MainActivity.this;
                TextView textViewMono3 = mainActivity3.mono("+", 26, mainActivity3.th.fgDim);
                textViewMono3.setGravity(17);
                textViewMono3.setPadding(MainActivity.this.dp(14), MainActivity.this.dp(2), MainActivity.this.dp(14), MainActivity.this.dp(2));
                linearLayout.addView(textViewMono3);
                textView = textViewMono3;
                textView2 = textViewMono2;
            }
            final Track track = (Track) MainActivity.this.shown.get(i);
            boolean z = MainActivity.this.svc != null && MainActivity.this.svc.current() == track;
            boolean zIsPlayable = track.isPlayable();
            Locale locale = Locale.US;
            Object[] objArr = new Object[3];
            objArr[0] = Integer.valueOf(i + 1);
            objArr[1] = z ? "▶" : "·";
            objArr[2] = track.title;
            textViewMono.setText(String.format(locale, "%02d %s %s", objArr));
            String upperCase = track.ext().toUpperCase(Locale.US);
            String strMmss = track.duration > 0.0d ? MainActivity.mmss((int) (track.duration * 1000.0d)) : "--:--";
            StringBuilder sb = new StringBuilder();
            sb.append(track.artist);
            sb.append(" · ");
            sb.append(upperCase);
            sb.append(" · ");
            sb.append(strMmss);
            if (track.year > 0) {
                str = " · " + track.year;
            }
            sb.append(str);
            textView2.setText(sb.toString());
            Theme theme = MainActivity.this.th;
            if (z) {
                i2 = theme.accent;
            } else {
                i2 = zIsPlayable ? theme.fg : theme.fgDim;
            }
            textViewMono.setTextColor(i2);
            Theme theme2 = MainActivity.this.th;
            textView2.setTextColor(z ? theme2.accent : theme2.fgDim);
            textViewMono.setTypeface(MainActivity.this.mono);
            textView2.setTypeface(MainActivity.this.mono);
            textView.setTypeface(MainActivity.this.mono);
            textView.setTextColor(MainActivity.this.th.fgDim);
            textView.setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$TrackAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view2) {
                    TrackAdapter.this.m91lambda$getView$0$netradiovoidbbsMainActivity$TrackAdapter(track, view2);
                }
            });
            linearLayout.setBackgroundColor(z ? MainActivity.this.th.bg2 : 0);
            return linearLayout;
        }

        /* JADX INFO: renamed from: lambda$getView$0$net-radiovoid-bbs-MainActivity$TrackAdapter, reason: not valid java name */
        /* synthetic */ void m91lambda$getView$0$netradiovoidbbsMainActivity$TrackAdapter(Track track, View view) {
            MainActivity.this.addToPlaylistDialog(track);
        }
    }

    // ===== chat feature helpers (replies / timestamps / nick colors / NP share) =====

    static long chatRefId(String body) {
        if (body == null || !body.startsWith(">>")) {
            return 0L;
        }
        int i = 2;
        long id = 0;
        while (i < body.length() && Character.isDigit(body.charAt(i))) {
            id = (id * 10) + (body.charAt(i) - '0');
            i++;
        }
        return i > 2 ? id : 0L;
    }

    static String chatStripRef(String body) {
        if (chatRefId(body) == 0) {
            return body;
        }
        int i = 2;
        while (i < body.length() && Character.isDigit(body.charAt(i))) {
            i++;
        }
        while (i < body.length() && body.charAt(i) == ' ') {
            i++;
        }
        return body.substring(i);
    }

    JSONObject chatFindMsg(long id) {
        for (JSONObject m : this.chatMsgs) {
            if (m.optLong("id") == id) {
                return m;
            }
        }
        return null;
    }

    // Same handle -> same hue as the website (hsl(hash%360, 85%, 65%)).
    static int nickColor(String nick) {
        int h = 0;
        String s = nick == null ? "" : nick;
        for (int i = 0; i < s.length(); i++) {
            h = (h * 31) + s.charAt(i);
        }
        float hue = (float) ((h & 0xFFFFFFFFL) % 360L);
        return hslColor(hue, 0.85f, 0.65f);
    }

    static int hslColor(float h, float s, float l) {
        float c = (1.0f - Math.abs((2.0f * l) - 1.0f)) * s;
        float x = c * (1.0f - Math.abs(((h / 60.0f) % 2.0f) - 1.0f));
        float m = l - (c / 2.0f);
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        return Color.rgb(Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255));
    }

    static OffsetDateTime chatWhen(JSONObject m) {
        try {
            return OffsetDateTime.parse(m.optString("created_at", ""));
        } catch (Exception e) {
            return null;
        }
    }

    static String chatTimeLocal(JSONObject m) {
        OffsetDateTime t = chatWhen(m);
        if (t == null) {
            String s = m.optString("created_at", "");
            return s.length() >= 16 ? s.substring(11, 16) : "--:--";
        }
        return t.atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    static String chatDayKey(JSONObject m) {
        OffsetDateTime t = chatWhen(m);
        if (t == null) {
            String s = m.optString("created_at", "");
            return s.length() >= 10 ? s.substring(0, 10) : "";
        }
        return t.atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    static String chatDayLabel(JSONObject m) {
        String key = chatDayKey(m);
        ZoneId z = ZoneId.systemDefault();
        String today = OffsetDateTime.now(z).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String yest = OffsetDateTime.now(z).minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        if (key.equals(today)) {
            return "TODAY";
        }
        if (key.equals(yest)) {
            return "YESTERDAY";
        }
        OffsetDateTime t = chatWhen(m);
        if (t != null) {
            return t.atZoneSameInstant(z).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)).toUpperCase(Locale.US);
        }
        return key;
    }

    boolean chatIsMention(JSONObject m, String body) {
        String me = nick();
        if (me.isEmpty() || m.optString("nick").equals(me)) {
            return false;
        }
        if (body.toLowerCase(Locale.US).contains("@" + me.toLowerCase(Locale.US))) {
            return true;
        }
        JSONObject ref = chatFindMsg(chatRefId(m.optString("body", "")));
        return ref != null && ref.optString("nick").equals(me);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startReply(JSONObject m) {
        this.chatReplyTo = m;
        if (this.chatReplyTxt != null) {
            String excerpt = chatStripRef(m.optString("body", ""));
            if (excerpt.length() > 60) {
                excerpt = excerpt.substring(0, 60) + "…";
            }
            this.chatReplyTxt.setText("↪ replying to <" + m.optString("nick", "?") + "> " + excerpt);
        }
        LinearLayout bar = this.chatReplyBar;
        if (bar != null) {
            bar.setVisibility(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelReply() {
        this.chatReplyTo = null;
        LinearLayout bar = this.chatReplyBar;
        if (bar != null) {
            bar.setVisibility(8);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void npShare() {
        PlayerService playerService = this.svc;
        Track t = playerService != null ? playerService.current() : null;
        if (t == null) {
            flash("NOTHING PLAYING");
            return;
        }
        String body = "♫ " + t.title + (t.artist == null || t.artist.isEmpty() ? "" : " — " + t.artist);
        if (body.length() > 280) {
            body = body.substring(0, 280);
        }
        Net.sendMessage(nick(), body, new MainActivity$$ExternalSyntheticLambda10(this), new Consumer() {
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MainActivity.this.flashSendFailed();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void flashSendFailed() {
        flash("CHAT SEND FAILED");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void npPlay(String bodyNoRef) {
        String s = bodyNoRef.startsWith("♫ ") ? bodyNoRef.substring(2) : bodyNoRef;
        String sl = s.toLowerCase(Locale.US);
        int best = -1;
        int bestLen = 0;
        for (int i = 0; i < this.allTracks.size(); i++) {
            String tt = this.allTracks.get(i).title;
            if (tt != null && !tt.isEmpty() && sl.startsWith(tt.toLowerCase(Locale.US)) && tt.length() > bestLen) {
                best = i;
                bestLen = tt.length();
            }
        }
        if (best < 0 || this.svc == null) {
            flash("TRACK NOT IN LIBRARY");
            return;
        }
        this.svc.setQueueAndPlay(new ArrayList(this.allTracks), best);
        toggleChat();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void chatMsgTapped(JSONObject m) {
        final String bodyNoRef = chatStripRef(m.optString("body", ""));
        final boolean isNp = bodyNoRef.startsWith("♫ ");
        final JSONObject msg = m;
        ArrayList<String> opts = new ArrayList<>();
        opts.add("↪ REPLY");
        if (isNp) {
            opts.add("▶ PLAY THIS TRACK");
        }
        new AlertDialog.Builder(this)
            .setTitle("<" + m.optString("nick", "?") + "> " + (bodyNoRef.length() > 40 ? bodyNoRef.substring(0, 40) + "…" : bodyNoRef))
            .setItems(opts.toArray(new String[0]), new DialogInterface.OnClickListener() {
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int which) {
                    if (which == 0) {
                        MainActivity.this.startReply(msg);
                    } else if (isNp) {
                        MainActivity.this.npPlay(bodyNoRef);
                    }
                }
            })
            .setNegativeButton("CANCEL", (DialogInterface.OnClickListener) null)
            .show();
    }

    private class ChatAdapter extends BaseAdapter {
        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return i;
        }

        private ChatAdapter() {
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return MainActivity.this.chatMsgs.size();
        }

        @Override // android.widget.Adapter
        public Object getItem(int i) {
            return MainActivity.this.chatMsgs.get(i);
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            MainActivity mainActivity = MainActivity.this;
            JSONObject m = mainActivity.chatMsgs.get(i);
            LinearLayout col = new LinearLayout(mainActivity);
            col.setOrientation(1);
            col.setPadding(mainActivity.dp(4), mainActivity.dp(1), mainActivity.dp(4), mainActivity.dp(1));

            // day separator when the local date changes
            String day = chatDayKey(m);
            String prevDay = i > 0 ? chatDayKey(mainActivity.chatMsgs.get(i - 1)) : null;
            if (!day.isEmpty() && !day.equals(prevDay)) {
                TextView sep = mainActivity.mono("── " + chatDayLabel(m) + " ──", 12, mainActivity.th.fgDim);
                sep.setGravity(Gravity.CENTER_HORIZONTAL);
                sep.setPadding(0, mainActivity.dp(6), 0, mainActivity.dp(3));
                col.addView(sep);
            }

            String rawBody = m.optString("body", "");
            long refId = chatRefId(rawBody);
            String body = chatStripRef(rawBody);

            // quoted message this one replies to
            if (refId > 0) {
                JSONObject ref = mainActivity.chatFindMsg(refId);
                String q;
                if (ref != null) {
                    String ex = chatStripRef(ref.optString("body", ""));
                    if (ex.length() > 48) {
                        ex = ex.substring(0, 48) + "…";
                    }
                    q = "↪ <" + ref.optString("nick", "?") + "> " + ex;
                } else {
                    q = "↪ >>" + refId;
                }
                TextView quote = mainActivity.mono(q, 13, mainActivity.th.fgDim);
                quote.setPadding(mainActivity.dp(14), 0, 0, 0);
                col.addView(quote);
            }

            boolean isNp = body.startsWith("♫ ");
            String time = chatTimeLocal(m);
            String nickStr = m.optString("nick", "?");
            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append(time, new ForegroundColorSpan(mainActivity.th.fgDim), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(" ");
            sb.append("<" + nickStr + ">", new ForegroundColorSpan(nickColor(nickStr)), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(" ");
            sb.append(body, new ForegroundColorSpan(isNp ? mainActivity.th.accent : mainActivity.th.fg), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            TextView tv = mainActivity.mono("", 15, mainActivity.th.fg);
            tv.setText(sb);
            col.addView(tv);

            if (mainActivity.chatIsMention(m, body)) {
                col.setBackgroundColor(mainActivity.th.bg2);
            }
            return col;
        }
    }

    @Override // net.radiovoid.bbs.PlayerService.Listener
    public void onState() {
        PlayerService playerService;
        String str;
        String str2;
        if (this.npTitle == null || (playerService = this.svc) == null) {
            return;
        }
        Track trackCurrent = playerService.current();
        if (trackCurrent != null) {
            TextView textView = this.npTitle;
            if (this.svc.isPreparing()) {
                str = "BUFFERING: " + trackCurrent.title;
            } else {
                str = trackCurrent.title;
            }
            textView.setText(str);
            TextView textView2 = this.npArtist;
            StringBuilder sb = new StringBuilder();
            sb.append(trackCurrent.artist);
            sb.append(" · ");
            sb.append(trackCurrent.ext().toUpperCase(Locale.US));
            if (trackCurrent.bpm > 0) {
                str2 = " · " + trackCurrent.bpm + " BPM";
            } else {
                str2 = "";
            }
            sb.append(str2);
            textView2.setText(sb.toString());
        }
        this.bPlay.setText(this.svc.isPlaying() ? "[ PAUSE ]" : "[ PLAY ]");
        this.bShuf.setTextColor(this.svc.shuffle ? this.th.accent : this.th.fg);
        this.bRpt.setTextColor(this.svc.repeat ? this.th.accent : this.th.fg);
        this.viz.setActive(this.svc.isPlaying());
        this.adapter.notifyDataSetChanged();
    }

    @Override // net.radiovoid.bbs.PlayerService.Listener
    public void onPcm(short[] sArr, int i) {
        VizView vizView = this.viz;
        if (vizView != null) {
            vizView.feedPcm(sArr, i);
        }
    }

    @Override // net.radiovoid.bbs.PlayerService.Listener
    public void onError(String str) {
        flash(str);
    }

    private void flash(String str) {
        this.flashMsg = str;
        this.flashUntil = System.currentTimeMillis() + 3500;
        TextView textView = this.statusLine;
        if (textView != null) {
            textView.setText("! " + str);
            this.statusLine.setTextColor(this.th.accent);
        }
    }

    private void showAbout() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(1);
        int iDp = dp(20);
        linearLayout.setPadding(iDp, iDp, iDp, iDp);
        linearLayout.setBackgroundColor(this.th.bg);
        try {
            LinearLayout linearLayoutRow = row();
            linearLayoutRow.setGravity(17);
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("logo.png")));
            linearLayoutRow.addView(imageView, new LinearLayout.LayoutParams(dp(84), dp(94)));
            ImageView imageView2 = new ImageView(this);
            imageView2.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("avatar.png")));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(84), dp(94));
            layoutParams.leftMargin = dp(16);
            linearLayoutRow.addView(imageView2, layoutParams);
            linearLayout.addView(linearLayoutRow);
        } catch (Exception unused) {
        }
        TextView textViewMono = mono("RADIOVOID BBS", 24, this.th.fgBright);
        textViewMono.setGravity(17);
        linearLayout.addView(textViewMono, lpTop(10));
        TextView textViewMono2 = mono("underground music terminal · native edition", 13, this.th.fgDim);
        textViewMono2.setGravity(17);
        linearLayout.addView(textViewMono2);
        TextView textViewMono3 = mono("created by MICKEY PERRY", 17, this.th.accent);
        textViewMono3.setGravity(17);
        linearLayout.addView(textViewMono3, lpTop(10));
        TextView textViewMono4 = mono("Motion Designer & Animator", 13, this.th.fgDim);
        textViewMono4.setGravity(17);
        linearLayout.addView(textViewMono4);
        new AlertDialog.Builder(this).setView(linearLayout).setPositiveButton("VISIT SITE", new DialogInterface.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda4
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m76lambda$showAbout$90$netradiovoidbbsMainActivity(dialogInterface, i);
            }
        }).setNegativeButton("CLOSE", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: renamed from: lambda$showAbout$90$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m76lambda$showAbout$90$netradiovoidbbsMainActivity(DialogInterface dialogInterface, int i) {
        try {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://mickeyperry.github.io/")));
        } catch (Exception unused) {
        }
    }

    private void showDebugLog() {
        TextView textViewMono = mono(PlayerService.dumpLog(), 12, this.th.fg);
        textViewMono.setPadding(dp(12), dp(12), dp(12), dp(12));
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(textViewMono);
        new AlertDialog.Builder(this).setTitle("DEBUG LOG").setView(scrollView).setPositiveButton("OK", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateVolumeUi() {
        String str;
        FrameLayout frameLayout = this.volBg;
        if (frameLayout == null || frameLayout.getWidth() == 0) {
            return;
        }
        int streamMaxVolume = this.audio.getStreamMaxVolume(3);
        int streamVolume = this.audio.getStreamVolume(3);
        int width = this.volBg.getWidth();
        int i = streamMaxVolume > 0 ? (width * streamVolume) / streamMaxVolume : 0;
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.volFill.getLayoutParams();
        layoutParams.width = i;
        this.volFill.setLayoutParams(layoutParams);
        FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) this.volThumb.getLayoutParams();
        layoutParams2.leftMargin = Math.max(0, Math.min(i - dp(4), width - dp(8)));
        this.volThumb.setLayoutParams(layoutParams2);
        TextView textView = this.volPct;
        if (streamMaxVolume > 0) {
            str = ((streamVolume * 100) / streamMaxVolume) + "%";
        } else {
            str = "--%";
        }
        textView.setText(str);
    }

    private void maybeAskBatteryExemption() {
        if (this.prefs.getBoolean("asked_batt", false) || ((PowerManager) getSystemService(PowerManager.class)).isIgnoringBatteryOptimizations(getPackageName())) {
            return;
        }
        this.prefs.edit().putBoolean("asked_batt", true).apply();
        new AlertDialog.Builder(this).setTitle("KEEP MUSIC ALIVE").setMessage("Your phone may kill background playback to save battery. Allow RadioVoid to ignore battery optimizations so the music keeps playing with the screen off.").setPositiveButton("ALLOW", new DialogInterface.OnClickListener() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda46
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m71xf1524a02(dialogInterface, i);
            }
        }).setNegativeButton("SKIP", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: renamed from: lambda$maybeAskBatteryExemption$91$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m71xf1524a02(DialogInterface dialogInterface, int i) {
        try {
            try {
                startActivity(new Intent("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", Uri.parse("package:" + getPackageName())));
            } catch (Exception unused) {
            }
        } catch (Exception unused2) {
            startActivity(new Intent("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"));
        }
    }

    private void cycleTheme() {
        this.themeIdx = (this.themeIdx + 1) % Theme.ALL.length;
        this.th = Theme.ALL[this.themeIdx];
        this.prefs.edit().putInt("theme", this.themeIdx).apply();
        applyTheme();
    }

    private void applyTheme() {
        Iterator<Runnable> it = this.themeAppliers.iterator();
        while (it.hasNext()) {
            it.next().run();
        }
        TrackAdapter trackAdapter = this.adapter;
        if (trackAdapter != null) {
            trackAdapter.notifyDataSetChanged();
        }
        ChatAdapter chatAdapter = this.chatAdapter;
        if (chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }
        getWindow().setStatusBarColor(this.th.bg);
        getWindow().setNavigationBarColor(this.th.bg);
    }

    private void themed(View view, Runnable runnable) {
        this.themeAppliers.add(runnable);
        runnable.run();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public TextView mono(String str, int i, int i2) {
        TextView textView = new TextView(this);
        textView.setTypeface(this.mono);
        textView.setText(str);
        textView.setTextSize(i);
        textView.setTextColor(i2);
        return textView;
    }

    private TextView btn(String str) {
        final TextView textViewMono = mono(str, 16, this.th.fg);
        int iDp = dp(10);
        int iDp2 = dp(5);
        textViewMono.setPadding(iDp, iDp2, iDp, iDp2);
        textViewMono.setGravity(17);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.leftMargin = dp(4);
        textViewMono.setLayoutParams(layoutParams);
        themed(textViewMono, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda23
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m7lambda$btn$92$netradiovoidbbsMainActivity(textViewMono);
            }
        });
        textViewMono.setClickable(true);
        return textViewMono;
    }

    /* JADX INFO: renamed from: lambda$btn$92$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m7lambda$btn$92$netradiovoidbbsMainActivity(TextView textView) {
        textView.setTextColor(this.th.fg);
        textView.setBackground(stroke(this.th.fg, this.th.bg2));
    }

    private GradientDrawable stroke(int i, int i2) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i2);
        gradientDrawable.setStroke(dp(1), i);
        return gradientDrawable;
    }

    private LinearLayout row() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        return linearLayout;
    }

    private LinearLayout box() {
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        int iDp = dp(8);
        linearLayout.setPadding(iDp, iDp, iDp, iDp);
        themed(linearLayout, new Runnable() { // from class: net.radiovoid.bbs.MainActivity$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m6lambda$box$93$netradiovoidbbsMainActivity(linearLayout);
            }
        });
        return linearLayout;
    }

    /* JADX INFO: renamed from: lambda$box$93$net-radiovoid-bbs-MainActivity, reason: not valid java name */
    /* synthetic */ void m6lambda$box$93$netradiovoidbbsMainActivity(LinearLayout linearLayout) {
        linearLayout.setBackground(stroke(this.th.fg, this.th.bg));
    }

    private LinearLayout.LayoutParams lpw(int i, float f) {
        return new LinearLayout.LayoutParams(i, -2, f);
    }

    private LinearLayout.LayoutParams lpTop(int i) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(i);
        return layoutParams;
    }

    private LinearLayout.LayoutParams lpTopH(int i, int i2) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, i2);
        layoutParams.topMargin = dp(i);
        return layoutParams;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dp(int i) {
        return (int) ((i * getResources().getDisplayMetrics().density) + 0.5f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String mmss(int i) {
        int iMax = Math.max(i, 0) / 1000;
        return String.format(Locale.US, "%02d:%02d", Integer.valueOf(iMax / 60), Integer.valueOf(iMax % 60));
    }
}
