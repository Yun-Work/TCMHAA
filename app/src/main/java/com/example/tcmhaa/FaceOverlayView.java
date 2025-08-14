package com.example.tcmhaa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class FaceOverlayView extends View {

    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ovalRect = new RectF();

    public FaceOverlayView(Context c) { super(c); init(); }
    public FaceOverlayView(Context c, AttributeSet a) { super(c, a); init(); }
    public FaceOverlayView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        // 整體白底遮罩
        maskPaint.setColor(0xFFFFFFFF); // 純白

        // 透明洞
        holePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        // 外框線
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(6f);
        strokePaint.setColor(0xFF56C8D8); // 藍綠色外框

        // 讓 CLEAR 生效
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // 設計「臉部橢圓」的位置：置中，寬度約螢幕 65%，高度為寬的 80%
        float ovalWidth = w * 0.65f;
        float ovalHeight = ovalWidth * 0.80f;

        float cx = w / 2f;
        float cy = h / 2f - h * 0.06f; // 往上微調一點，讓下方空出拍照鍵

        ovalRect.set(
                cx - ovalWidth / 2f,
                cy - ovalHeight / 2f,
                cx + ovalWidth / 2f,
                cy + ovalHeight / 2f
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 先鋪滿白底
        canvas.drawRect(0, 0, getWidth(), getHeight(), maskPaint);

        // 挖出透明橢圓洞
        canvas.drawOval(ovalRect, holePaint);

        // 畫一圈外框線（在洞外緣）
        canvas.drawOval(ovalRect, strokePaint);
    }
}
