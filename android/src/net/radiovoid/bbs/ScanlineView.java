package net.radiovoid.bbs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
public class ScanlineView extends View {
    private final Paint line;
    private final Paint vig;

    public ScanlineView(Context context) {
        super(context);
        Paint paint = new Paint();
        this.line = paint;
        this.vig = new Paint();
        paint.setColor(Color.argb(36, 0, 0, 0));
        setClickable(false);
        setFocusable(false);
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        if (i <= 0 || i2 <= 0) {
            return;
        }
        this.vig.setShader(new RadialGradient(i / 2.0f, i2 / 2.0f, Math.max(i, i2) * 0.75f, new int[]{0, 0, Color.argb(140, 0, 0, 0)}, new float[]{0.0f, 0.6f, 1.0f}, Shader.TileMode.CLAMP));
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        int height = getHeight();
        int width = getWidth();
        for (int i = 0; i < height; i += 4) {
            float f = i;
            canvas.drawRect(0.0f, f, width, f + 1.5f, this.line);
        }
        canvas.drawRect(0.0f, 0.0f, width, height, this.vig);
    }
}
