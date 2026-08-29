package net.radiovoid.bbs;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class OpenmptEngine implements ModEngine {
    private static final int CHUNK = 2048;
    private static final int RATE = 48000;
    private final ModEngine.Callback cb;
    private final int durationMs;
    private final Object lock = new Object();
    private final long mod;
    private volatile boolean paused;
    private volatile boolean released;
    private volatile boolean started;
    private Thread thread;
    private final AudioTrack track;

    @Override // net.radiovoid.bbs.ModEngine
    public boolean canSeek() {
        return true;
    }

    public OpenmptEngine(byte[] bArr, ModEngine.Callback callback) throws IOException {
        this.cb = callback;
        long jCreate = Openmpt.create(bArr);
        this.mod = jCreate;
        if (jCreate == 0) {
            throw new IOException("libopenmpt could not parse module");
        }
        this.durationMs = (int) (Openmpt.duration(jCreate) * 1000.0d);
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
        Thread thread = new Thread(new Runnable() { // from class: net.radiovoid.bbs.OpenmptEngine$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                OpenmptEngine.this.run();
            }
        }, "openmpt-mixer");
        this.thread = thread;
        thread.setPriority(10);
        this.thread.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void run() {
        int i;
        short[] sArr = new short[4096];
        while (!this.released) {
            if (this.paused) {
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException unused) {
                    return;
                }
            } else {
                synchronized (this.lock) {
                    i = Openmpt.read(this.mod, RATE, sArr, CHUNK);
                }
                if (i <= 0) {
                    if (this.released) {
                        return;
                    }
                    this.cb.onComplete();
                    return;
                }
                this.cb.onPcm(sArr, i);
                int i2 = i * 2;
                int i3 = 0;
                while (i3 < i2 && !this.released && !this.paused) {
                    int iWrite = this.track.write(sArr, i3, i2 - i3);
                    if (iWrite <= 0) {
                        break;
                    } else {
                        i3 += iWrite;
                    }
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
        int iPosition;
        synchronized (this.lock) {
            iPosition = (int) (Openmpt.position(this.mod) * 1000.0d);
        }
        return iPosition;
    }

    @Override // net.radiovoid.bbs.ModEngine
    public int durationMs() {
        return this.durationMs;
    }

    @Override // net.radiovoid.bbs.ModEngine
    public void seekMs(int i) {
        synchronized (this.lock) {
            Openmpt.seek(this.mod, ((double) i) / 1000.0d);
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
        synchronized (this.lock) {
            Openmpt.destroy(this.mod);
        }
    }
}
