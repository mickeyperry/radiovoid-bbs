package net.radiovoid.bbs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
public class VizView extends View {
    private static final int FFT_N = 512;
    public static final String[] MODES = {"BARS", "WAVE", "SCOPE"};
    private static final int NBARS = 40;
    private static final int WIN = 1024;
    private boolean active;
    private final float[] bars;
    private final float[] hann;
    private final float[] im;
    private long lastFrame;
    private long lastPcm;
    private final Object lock;
    private final float[] mag;
    private int mode;
    private Runnable onModeChange;
    private final Paint paint;
    private final Path path;
    private final float[] peaks;
    private final float[] re;
    private Theme theme;
    private final float[] window;

    public VizView(Context context) {
        super(context);
        this.paint = new Paint(1);
        this.path = new Path();
        this.window = new float[WIN];
        this.bars = new float[NBARS];
        this.peaks = new float[NBARS];
        this.re = new float[FFT_N];
        this.im = new float[FFT_N];
        this.mag = new float[256];
        this.hann = new float[FFT_N];
        this.lock = new Object();
        this.theme = Theme.ALL[0];
        for (int i = 0; i < FFT_N; i++) {
            this.hann[i] = (float) (0.5d - (Math.cos((((double) i) * 6.283185307179586d) / 511.0d) * 0.5d));
        }
        setOnClickListener(new View.OnClickListener() { // from class: net.radiovoid.bbs.VizView$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                VizView.this.m101lambda$new$0$netradiovoidbbsVizView(view);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$new$0$net-radiovoid-bbs-VizView, reason: not valid java name */
    /* synthetic */ void m101lambda$new$0$netradiovoidbbsVizView(View view) {
        this.mode = (this.mode + 1) % MODES.length;
        Runnable runnable = this.onModeChange;
        if (runnable != null) {
            runnable.run();
        }
        invalidate();
    }

    public void setOnModeChange(Runnable runnable) {
        this.onModeChange = runnable;
    }

    public String modeName() {
        return MODES[this.mode];
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
        invalidate();
    }

    public void setActive(boolean z) {
        boolean z2 = this.active;
        this.active = z;
        if (!z || z2) {
            return;
        }
        postInvalidateOnAnimation();
    }

    public void feedPcm(short[] sArr, int i) {
        synchronized (this.lock) {
            int iMin = Math.min(i, WIN);
            int i2 = 1024 - iMin;
            float[] fArr = this.window;
            System.arraycopy(fArr, iMin, fArr, 0, i2);
            for (int i3 = 0; i3 < iMin; i3++) {
                int i4 = i3 * 2;
                this.window[i2 + i3] = ((sArr[i4] + sArr[i4 + 1]) / 2) / 32768.0f;
            }
            this.lastPcm = System.currentTimeMillis();
        }
        postInvalidateOnAnimation();
    }

    private static void fft(float[] fArr, float[] fArr2) {
        int length = fArr.length;
        int i = 0;
        for (int i2 = 1; i2 < length; i2++) {
            int i3 = length >> 1;
            while ((i & i3) != 0) {
                i ^= i3;
                i3 >>= 1;
            }
            i ^= i3;
            if (i2 < i) {
                float f = fArr[i2];
                fArr[i2] = fArr[i];
                fArr[i] = f;
                float f2 = fArr2[i2];
                fArr2[i2] = fArr2[i];
                fArr2[i] = f2;
            }
        }
        for (int i4 = 2; i4 <= length; i4 <<= 1) {
            double d = (-6.283185307179586d) / ((double) i4);
            float fCos = (float) Math.cos(d);
            float fSin = (float) Math.sin(d);
            for (int i5 = 0; i5 < length; i5 += i4) {
                float f3 = 1.0f;
                float f4 = 0.0f;
                int i7 = i4 / 2;
                for (int i6 = 0; i6 < i7; i6++) {
                    int i8 = i5 + i6;
                    int i9 = i7 + i8;
                    float f5 = fArr[i9];
                    float f6 = fArr2[i9];
                    float f7 = (f5 * f3) - (f6 * f4);
                    float f8 = (f5 * f4) + (f6 * f3);
                    fArr[i9] = fArr[i8] - f7;
                    fArr2[i9] = fArr2[i8] - f8;
                    fArr[i8] = fArr[i8] + f7;
                    fArr2[i8] = fArr2[i8] + f8;
                    float f9 = (f3 * fCos) - (f4 * fSin);
                    f4 = (f4 * fCos) + (f3 * fSin);
                    f3 = f9;
                }
            }
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        float[] fArr;
        long jCurrentTimeMillis = System.currentTimeMillis();
        boolean z = jCurrentTimeMillis - this.lastPcm < 400;
        long j = this.lastFrame;
        float fMin = j == 0 ? 0.016f : Math.min((jCurrentTimeMillis - j) / 1000.0f, 0.05f);
        this.lastFrame = jCurrentTimeMillis;
        int width = getWidth();
        int height = getHeight();
        canvas.drawColor(this.theme.bg);
        synchronized (this.lock) {
            fArr = (float[]) this.window.clone();
        }
        int i = this.mode;
        if (i == 0) {
            drawBars(canvas, width, height, fArr, z, fMin);
        } else if (i == 1) {
            drawWave(canvas, width, height, fArr, z);
        } else {
            drawScope(canvas, width, height, fArr, z);
        }
        if (this.active || z) {
            postInvalidateOnAnimation();
        }
    }

    private void drawBars(Canvas canvas, int i, int i2, float[] fArr, boolean z, float f) {
        float fPow;
        if (z) {
            for (int i3 = 0; i3 < FFT_N; i3++) {
                this.re[i3] = fArr[i3 + FFT_N] * this.hann[i3];
                this.im[i3] = 0.0f;
            }
            fft(this.re, this.im);
            for (int i4 = 0; i4 < 256; i4++) {
                float[] fArr2 = this.mag;
                float f2 = this.re[i4];
                float f3 = this.im[i4];
                fArr2[i4] = ((float) Math.sqrt((f2 * f2) + (f3 * f3))) / 128.0f;
            }
        }
        for (int i5 = 0; i5 < NBARS; i5++) {
            if (z) {
                int iPow = ((int) Math.pow(128.0d, i5 / 40.0f)) * 2;
                int iPow2 = ((int) Math.pow(128.0d, (i5 + 1) / 40.0f)) * 2;
                if (iPow2 <= iPow) {
                    iPow2 = iPow + 1;
                }
                if (iPow2 > 256) {
                    iPow2 = 256;
                }
                float fMax = 0.0f;
                while (iPow < iPow2) {
                    fMax = Math.max(fMax, this.mag[iPow]);
                    iPow++;
                }
                fPow = (float) Math.pow(Math.min(fMax * 2.2f, 1.0f), 0.7d);
            } else {
                fPow = 0.0f;
            }
            float[] fArr3 = this.bars;
            float f4 = fArr3[i5];
            if (fPow > f4) {
                fArr3[i5] = f4 + ((fPow - f4) * 0.6f);
            } else {
                fArr3[i5] = Math.max(0.0f, f4 - (2.4f * f));
            }
            float f5 = this.bars[i5];
            float[] fArr4 = this.peaks;
            float f6 = fArr4[i5];
            if (f5 > f6) {
                fArr4[i5] = f5;
            } else {
                fArr4[i5] = Math.max(0.0f, f6 - (0.5f * f));
            }
        }
        float f7 = i / 40.0f;
        this.paint.setStyle(Paint.Style.FILL);
        for (int i6 = 0; i6 < NBARS; i6++) {
            float f8 = i2 - 8;
            float f9 = i6 * f7;
            int i7 = (int) ((this.bars[i6] * f8) / 6.0f);
            for (int i8 = 0; i8 < i7; i8++) {
                float f10 = (i8 * 6.0f) / f8;
                this.paint.setColor(f10 > 0.72f ? this.theme.viz3 : f10 > 0.42f ? this.theme.viz2 : this.theme.viz1);
                float f11 = (i2 - 4) - (i8 * 6);
                canvas.drawRect(f9 + 1.0f, f11 - 4.0f, (f9 + f7) - 2.0f, f11, this.paint);
            }
            this.paint.setColor(this.theme.fgBright);
            float f12 = (i2 - 4) - (this.peaks[i6] * f8);
            canvas.drawRect(f9 + 1.0f, f12 - 2.0f, (f9 + f7) - 2.0f, f12, this.paint);
        }
    }

    private void drawWave(Canvas canvas, int i, int i2, float[] fArr, boolean z) {
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(2.5f);
        this.paint.setColor(this.theme.viz1);
        this.path.reset();
        float f = i2 / 2.0f;
        for (int i3 = 0; i3 < i; i3 += 2) {
            float f2 = ((z ? fArr[(i3 * 1023) / Math.max(i - 1, 1)] : 0.0f) * (f - 6.0f)) + f;
            if (i3 == 0) {
                this.path.moveTo(i3, f2);
            } else {
                this.path.lineTo(i3, f2);
            }
        }
        canvas.drawPath(this.path, this.paint);
        this.paint.setStrokeWidth(1.0f);
        this.paint.setColor(this.theme.fgDim);
        canvas.drawLine(0.0f, f, i, f, this.paint);
    }

    private void drawScope(Canvas canvas, int i, int i2, float[] fArr, boolean z) {
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(this.theme.viz2);
        float f = i2 / 2.0f;
        for (int i3 = 0; i3 < 160; i3++) {
            float f2 = (i3 * i) / 160;
            float f3 = ((z ? fArr[(i3 * 1023) / 159] : 0.0f) * (f - 6.0f)) + f;
            canvas.drawRect(f2, f3 - 2.0f, f2 + 3.0f, f3 + 2.0f, this.paint);
        }
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setColor(this.theme.fgDim);
        canvas.drawRect(1.0f, 1.0f, i - 1, i2 - 1, this.paint);
    }
}
