package com.example.tcmhaa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class FaceOverlayView extends View {

    // 可微調的參數
    private static final float WIDTH_PERCENT_OF_SCREEN  = 0.68f; // 橢圓相對螢幕寬度的最大占比
    private static final float HEIGHT_PERCENT_OF_SCREEN = 0.8f; // 橢圓相對螢幕高度的最大占比
    private static final float MOVE_UP_RATIO            = 0.06f; // 往上微移比例（佔螢幕高）
    private static final float OVAL_ASPECT_W            = 5f;    // 寬
    private static final float OVAL_ASPECT_H            = 7f;    // 高（→ 高 > 寬）

    private final Paint maskPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint holePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ovalRect    = new RectF();

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
        strokePaint.setStrokeWidth(dp(3));               // 邊框粗細
        strokePaint.setColor(0xFF56C8D8);               // 藍綠色外框

        // 讓 CLEAR 生效
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // 安全邊距，避免貼邊（四周各留 24dp）
        float padding = dp(24);

        // 先依螢幕寬與高給一個「上限尺寸」
        float maxOvalW = Math.max(0, w * WIDTH_PERCENT_OF_SCREEN  - padding * 2);
        float maxOvalH = Math.max(0, h * HEIGHT_PERCENT_OF_SCREEN - padding * 2);

        // 依 3:4 比例計算實際寬高（以不超框為原則）
        // 先假設寬度受限，推算高度
        float targetW = maxOvalW;
        float targetH = targetW * (OVAL_ASPECT_H / OVAL_ASPECT_W);

        // 如果算出來的高度超過允許，就用高度受限再回推寬度
        if (targetH > maxOvalH) {
            targetH = maxOvalH;
            targetW = targetH * (OVAL_ASPECT_W / OVAL_ASPECT_H);
        }

        // 中心點：置中後往上移一點，底下給拍照鍵空間
        float cx = w / 2f;
        float cy = h / 2f - h * MOVE_UP_RATIO;

        ovalRect.set(
                cx - targetW / 2f,
                cy - targetH / 2f,
                cx + targetW / 2f,
                cy + targetH / 2f
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 先鋪滿白底
        canvas.drawRect(0, 0, getWidth(), getHeight(), maskPaint);

        // 挖出透明橢圓洞
        canvas.drawOval(ovalRect, holePaint);

        // 外框
        canvas.drawOval(ovalRect, strokePaint);
    }
}
