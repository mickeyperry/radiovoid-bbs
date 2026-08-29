package net.radiovoid.bbs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import java.util.Random;

/* JADX INFO: loaded from: classes.dex */
public class GlitchView extends View {
    private final String caption;
    private final long duration;
    private boolean finished;
    private final Runnable onDone;
    private final Paint paint;
    private final Random rnd;
    private long startAt;
    private final Paint textPaint;
    private final Theme th;

    public static void play(ViewGroup viewGroup, Theme theme, Typeface typeface, String str, long j, Runnable runnable) {
        viewGroup.addView(new GlitchView(viewGroup.getContext(), theme, typeface, str, j, runnable), new ViewGroup.LayoutParams(-1, -1));
    }

    private GlitchView(Context context, Theme theme, Typeface typeface, String str, long j, Runnable runnable) {
        super(context);
        this.paint = new Paint();
        Paint paint = new Paint(1);
        this.textPaint = paint;
        this.rnd = new Random();
        this.th = theme;
        this.caption = str;
        this.duration = j;
        this.onDone = runnable;
        paint.setTypeface(typeface);
        paint.setTextAlign(Paint.Align.CENTER);
        setClickable(true);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        if (this.startAt == 0) {
            this.startAt = System.currentTimeMillis();
        }
        float fCurrentTimeMillis = (System.currentTimeMillis() - this.startAt) / this.duration;
        if (fCurrentTimeMillis >= 1.0f) {
            if (this.finished) {
                return;
            }
            this.finished = true;
            post(new Runnable() { // from class: net.radiovoid.bbs.GlitchView$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GlitchView.this.m2lambda$onDraw$0$netradiovoidbbsGlitchView();
                }
            });
            return;
        }
        int width = getWidth();
        int height = getHeight();
        float f = fCurrentTimeMillis < 0.25f ? 4.0f * fCurrentTimeMillis : (1.0f - fCurrentTimeMillis) * 1.33f;
        this.paint.setColor(Color.argb((int) (140.0f * f), 0, 0, 0));
        float f2 = width;
        float f3 = height;
        canvas.drawRect(0.0f, 0.0f, f2, f3, this.paint);
        int i = 0;
        for (int i2 = ((int) (10.0f * f)) + 2; i < i2; i2 = i2) {
            int iNextInt = this.rnd.nextInt(Math.max(height, 1));
            int iNextInt2 = this.rnd.nextInt(24) + 4;
            int iNextInt3 = this.rnd.nextInt(81) - 40;
            int i3 = i % 3;
            this.paint.setColor(i3 == 0 ? this.th.accent : i3 == 1 ? this.th.fg : this.th.viz3);
            this.paint.setAlpha((int) (90.0f * f));
            canvas.drawRect(iNextInt3, iNextInt, iNextInt3 + width, iNextInt + iNextInt2, this.paint);
            i++;
        }
        for (int i4 = 0; i4 < 6; i4++) {
            int iNextInt4 = this.rnd.nextInt(Math.max(width, 1));
            this.paint.setColor(i4 % 2 == 0 ? -65536 : -16711681);
            this.paint.setAlpha((int) (50.0f * f));
            canvas.drawRect(iNextInt4, 0.0f, iNextInt4 + 2 + this.rnd.nextInt(5), f3, this.paint);
        }
        for (int i5 = 0; i5 < f * 14.0f; i5++) {
            this.paint.setColor(this.th.fgBright);
            this.paint.setAlpha(this.rnd.nextInt(60) + 20);
            int iNextInt5 = this.rnd.nextInt(60) + 8;
            int iNextInt6 = this.rnd.nextInt(8) + 2;
            int i6 = width - iNextInt5;
            canvas.drawRect(this.rnd.nextInt(Math.max(i6, 1)), this.rnd.nextInt(Math.max(height - iNextInt6, 1)), (this.rnd.nextInt(Math.max(i6, 1)) % width) + iNextInt5, (this.rnd.nextInt(Math.max(height, 1)) % height) + iNextInt6, this.paint);
        }
        if (this.caption != null && (this.rnd.nextInt(10) > 1 || fCurrentTimeMillis > 0.5f)) {
            this.textPaint.setTextSize(f3 / 14.0f);
            float fNextInt = (f2 / 2.0f) + ((this.rnd.nextInt(9) - 4) * f);
            float fNextInt2 = (f3 / 2.0f) + ((this.rnd.nextInt(9) - 4) * f);
            this.textPaint.setColor(this.th.accent);
            this.textPaint.setAlpha((int) ((f * 120.0f) + 60.0f));
            canvas.drawText(this.caption, 3.0f + fNextInt, 2.0f + fNextInt2, this.textPaint);
            this.textPaint.setColor(this.th.fgBright);
            this.textPaint.setAlpha(255);
            canvas.drawText(this.caption, fNextInt, fNextInt2, this.textPaint);
        }
        postInvalidateOnAnimation();
    }

    /* JADX INFO: renamed from: lambda$onDraw$0$net-radiovoid-bbs-GlitchView, reason: not valid java name */
    /* synthetic */ void m2lambda$onDraw$0$netradiovoidbbsGlitchView() {
        ViewGroup viewGroup = (ViewGroup) getParent();
        if (viewGroup != null) {
            viewGroup.removeView(this);
        }
        Runnable runnable = this.onDone;
        if (runnable != null) {
            runnable.run();
        }
    }
}
