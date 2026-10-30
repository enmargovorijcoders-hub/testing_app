package com.example.devicetest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** Draws a coloured trail for every finger, so dead zones and multi-touch problems show up. */
class TouchView extends View {
    private static final int[] COLORS = {0xFFEF4444, 0xFF22C55E, 0xFF3B82F6, 0xFFF59E0B, 0xFFEC4899,
            0xFF14B8A6, 0xFF8B5CF6, 0xFFF97316, 0xFFFFFFFF, 0xFF9AA4B2};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap bmp;
    private Canvas canvas;
    private int active;
    private int max;

    TouchView(Context c) {
        super(c);
        paint.setTextSize(42f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        bmp = Bitmap.createBitmap(Math.max(w, 1), Math.max(h, 1), Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bmp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int n = e.getPointerCount();
        if (n > max) max = n;
        int act = e.getActionMasked();
        active = (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) ? 0 : n;
        if (canvas != null) {
            for (int i = 0; i < n; i++) {
                paint.setColor(COLORS[e.getPointerId(i) % COLORS.length]);
                canvas.drawCircle(e.getX(i), e.getY(i), 10f + e.getPressure(i) * 20f, paint);
            }
        }
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        c.drawColor(0xFF000000);
        if (bmp != null) c.drawBitmap(bmp, 0, 0, null);
        paint.setColor(0xFFFFFFFF);
        c.drawText("Fingers: " + active + "   Most at once: " + max, 24f, 56f, paint);
    }
}
