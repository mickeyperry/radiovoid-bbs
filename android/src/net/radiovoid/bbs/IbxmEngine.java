package net.radiovoid.bbs;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import ibxm.IBXM;
import ibxm.Module;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class IbxmEngine implements ModEngine {
    private static final int RATE = 48000;
    private final ModEngine.Callback cb;
    private final int durationFrames;
    private volatile long framesPlayed;

    /* JADX INFO: renamed from: ibxm, reason: collision with root package name */
    private final IBXM f0ibxm;
    private final Object lock = new Object();
    private volatile boolean paused;
    private volatile boolean released;
    private volatile boolean started;
    private Thread thread;
    private final AudioTrack track;

    @Override // net.radiovoid.bbs.ModEngine
    public boolean canSeek() {
        return true;
    }

    public IbxmEngine(byte[] bArr, ModEngine.Callback callback) throws IOException {
        this.cb = callback;
        IBXM ibxm2 = new IBXM(new Module(bArr), RATE);
        this.f0ibxm = ibxm2;
        this.durationFrames = Math.max(ibxm2.calculateSongDuration(), 1);
        this.track = new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(1).setContentType(2).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(RATE).setEncoding(2).setChannelMask(12).build()).setBufferSizeInBytes(Math.max(AudioTrack.getMinBufferSize(RATE, 12, 2) * 2, RATE)).setTransferMode(1).build();
    }

    @Override // net.radiovoid.bbs.ModEngine
    public void start(boolean z) {
        if (this.started) {
            return;
        }
        this.started = true;
        this.paused = z;
        if (!z) {
            this.track.play();
        }
        Thread thread = new Thread(new Runnable() { // from class: net.radiovoid.bbs.IbxmEngine$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                IbxmEngine.this.run();
            }
        }, "ibxm-mixer");
        this.thread = thread;
        thread.setPriority(10);
        this.thread.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void run() {
        int audio;
        int mixBufferLength = this.f0ibxm.getMixBufferLength();
        int[] iArr = new int[mixBufferLength];
        short[] sArr = new short[mixBufferLength];
        while (!this.released) {
            if (this.paused) {
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException unused) {
                    return;
                }
            } else {
                synchronized (this.lock) {
                    audio = this.f0ibxm.getAudio(iArr);
                    this.framesPlayed += (long) audio;
                }
                int i = audio * 2;
                int i2 = 0;
                for (int i3 = 0; i3 < i; i3++) {
                    int i4 = iArr[i3];
                    if (i4 > 32767) {
                        i4 = 32767;
                    }
                    if (i4 < -32768) {
                        i4 = -32768;
                    }
                    sArr[i3] = (short) i4;
                }
                this.cb.onPcm(sArr, audio);
                while (i2 < i && !this.released && !this.paused) {
                    int iWrite = this.track.write(sArr, i2, i - i2);
                    if (iWrite <= 0) {
                        break;
                    } else {
                        i2 += iWrite;
                    }
                }
                if (this.framesPlayed >= this.durationFrames) {
                    if (this.released) {
                        return;
                    }
                    this.cb.onComplete();
                    return;
                }
            }
        }
    }

    @Override // net.radiovoid.bbs.ModEngine
    public void pause() {
        this.paused = true;
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
        this.paused = false;
        try {
            this.track.play();
        } catch (Exception unused) {
        }
    }

    @Override // net.radiovoid.bbs.ModEngine
    public boolean isPaused() {
        return this.paused || !this.started;
    }

    @Override // net.radiovoid.bbs.ModEngine
    public int positionMs() {
        return (int) ((this.framesPlayed * 1000) / 48000);
    }

    @Override // net.radiovoid.bbs.ModEngine
    public int durationMs() {
        return (int) ((((long) this.durationFrames) * 1000) / 48000);
    }

    @Override // net.radiovoid.bbs.ModEngine
    public void seekMs(int i) {
        synchronized (this.lock) {
            this.framesPlayed = this.f0ibxm.seek((int) Math.min(Math.max(0L, (((long) i) * 48000) / 1000), ((long) this.durationFrames) - 1));
        }
        try {
            this.track.flush();
        } catch (Exception unused) {
        }
    }

    @Override // net.radiovoid.bbs.ModEngine
    public void release() {
        this.released = true;
        Thread thread = this.thread;
        if (thread != null) {
            thread.interrupt();
            try {
                this.thread.join(500L);
            } catch (InterruptedException unused) {
            }
        }
        try {
            this.track.stop();
        } catch (Exception unused2) {
        }
        this.track.release();
    }
}
