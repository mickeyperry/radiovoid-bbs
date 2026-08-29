package net.radiovoid.bbs;

/* JADX INFO: loaded from: classes.dex */
public interface ModEngine {

    public interface Callback {
        void onComplete();

        void onPcm(short[] sArr, int i);
    }

    boolean canSeek();

    int durationMs();

    boolean isPaused();

    void pause();

    int positionMs();

    void release();

    void resume();

    void seekMs(int i);

    void start(boolean z);
}
