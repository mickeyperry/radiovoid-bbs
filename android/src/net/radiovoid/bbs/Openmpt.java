package net.radiovoid.bbs;

/* JADX INFO: loaded from: classes.dex */
public final class Openmpt {
    private static final boolean ok;

    public static native long create(byte[] bArr);

    public static native void destroy(long j);

    public static native double duration(long j);

    public static native double position(long j);

    public static native int read(long j, int i, short[] sArr, int i2);

    public static native void seek(long j, double d);

    public static native String title(long j);

    static {
        boolean z;
        try {
            System.loadLibrary("openmpt");
            System.loadLibrary("openmpt_jni");
            z = true;
        } catch (Throwable th) {
            PlayerService.log("libopenmpt unavailable: " + th.getMessage());
            z = false;
        }
        ok = z;
    }

    public static boolean available() {
        return ok;
    }
}
