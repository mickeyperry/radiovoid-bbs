package net.radiovoid.bbs;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.io.IOException;
import ozmod.ITPlayer;
import ozmod.OZModPlayer;

/* JADX INFO: loaded from: classes.dex */
public final class OzmodEngine implements ModEngine {
    private static final int RATE = 44100;
    private final ModEngine.Callback cb;
    private final int durationMsHint;
    private volatile long framesWritten;
    private volatile boolean pausedFlag;
    private final ITPlayer player;
    private volatile boolean released;
    private volatile boolean started;
    private final AudioTrack track = new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(1).setContentType(2).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(RATE).setEncoding(2).setChannelMask(12).build()).setBufferSizeInBytes(Math.max(AudioTrack.getMinBufferSize(RATE, 12, 2) * 2, RATE)).setTransferMode(1).build();

    @Override // net.radiovoid.bbs.ModEngine
    public boolean canSeek() {
        return false;
    }

    @Override // net.radiovoid.bbs.ModEngine
    public void seekMs(int i) {
    }

    static /* synthetic */ long access$214(OzmodEngine ozmodEngine, long j) {
        long j2 = ozmodEngine.framesWritten + j;
        ozmodEngine.framesWritten = j2;
        return j2;
    }

    public OzmodEngine(byte[] bArr, int i, final ModEngine.Callback callback) throws IOException {
        this.cb = callback;
        this.durationMsHint = i;
        ITPlayer iTPlayer = new ITPlayer(new OZModPlayer.IAudioDevice() { // from class: net.radiovoid.bbs.OzmodEngine.1
            @Override // ozmod.OZModPlayer.IAudioDevice
            public void dispose() {
            }

            @Override // ozmod.OZModPlayer.IAudioDevice
            public void setVolume(float f) {
            }

            @Override // ozmod.OZModPlayer.IAudioDevice
            public void writeSamples(short[] sArr, int i2, int i3) {
                if (OzmodEngine.this.released) {
                    return;
                }
                int i4 = i3 / 2;
                callback.onPcm(sArr, i4);
                while (i3 > 0 && !OzmodEngine.this.released) {
                    int iWrite = OzmodEngine.this.track.write(sArr, i2, i3);
                    if (iWrite <= 0) {
                        break;
                    }
                    i2 += iWrite;
                    i3 -= iWrite;
                }
                OzmodEngine.access$214(OzmodEngine.this, i4);
            }
        });
        this.player = iTPlayer;
        try {
            iTPlayer.load(bArr);
            iTPlayer.addWhenDone(new Runnable() { // from class: net.radiovoid.bbs.OzmodEngine$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    OzmodEngine.this.m92lambda$new$0$netradiovoidbbsOzmodEngine(callback);
                }
            });
        } catch (Throwable th) {
            this.track.release();
            throw new IOException("IT load failed: " + th.getMessage());
        }
    }

    /* JADX INFO: renamed from: lambda$new$0$net-radiovoid-bbs-OzmodEngine, reason: not valid java name */
    /* synthetic */ void m92lambda$new$0$netradiovoidbbsOzmodEngine(ModEngine.Callback callback) {
        if (this.released) {
            return;
        }
        callback.onComplete();
    }

    @Override // net.radiovoid.bbs.ModEngine
    public void start(boolean z) {
        if (this.started) {
            return;
        }
        this.started = true;
        this.pausedFlag = z;
        if (z) {
            this.player.pause(true);
        } else {
            this.track.play();
        }
        this.player.setPriority(7);
        this.player.play();
    }

    @Override // net.radiovoid.bbs.ModEngine
    public void pause() {
        this.pausedFlag = true;
        this.player.pause(true);
        try {
            this.track.pause();
        } catch (Exception unused) {
        }
    }

    @Override // net.radiovoid.bbs.ModEngine
    public void resume() {
        if (!this.started) {
            start(false);
            return;
        }
        this.pausedFlag = false;
        try {
            this.track.play();
        } catch (Exception unused) {
        }
        this.player.pause(false);
    }

    @Override // net.radiovoid.bbs.ModEngine
    public boolean isPaused() {
        return this.pausedFlag || !this.started;
    }

    @Override // net.radiovoid.bbs.ModEngine
    public int positionMs() {
        return (int) ((this.framesWritten * 1000) / 44100);
    }

    @Override // net.radiovoid.bbs.ModEngine
    public int durationMs() {
        return this.durationMsHint;
    }

    @Override // net.radiovoid.bbs.ModEngine
    public void release() {
        this.released = true;
        try {
            this.player.cancel();
        } catch (Exception unused) {
        }
        try {
            this.player.join(600L);
        } catch (InterruptedException unused2) {
        }
        try {
            this.track.stop();
        } catch (Exception unused3) {
        }
        this.track.release();
    }
}
