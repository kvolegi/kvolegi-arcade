package arcade.android;

import android.graphics.*;
import arcade.Gfx;

public class AndroidGfx implements Gfx {
    private Canvas canvas;
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF rectF = new RectF();
    private final Path path = new Path();

    private Typeface fontRegular = Typeface.DEFAULT;
    private Typeface fontBold = Typeface.DEFAULT_BOLD;

    public AndroidGfx() {
        fillPaint.setStyle(Paint.Style.FILL);
        strokePaint.setStyle(Paint.Style.STROKE);
    }

    public void setTypefaces(Typeface regular, Typeface bold) {
        if (regular != null) this.fontRegular = regular;
        if (bold != null) this.fontBold = bold;
    }

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    private int argb(int c) {
        int a = (c >>> 24) & 0xFF;
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void color(int c) {
        fillPaint.setShader(null);
        strokePaint.setShader(null);
        int col = argb(c);
        fillPaint.setColor(col);
        strokePaint.setColor(col);
    }

    @Override
    public void rect(float x, float y, float w, float h) {
        if (canvas != null) canvas.drawRect(x, y, x + w, y + h, fillPaint);
    }

    @Override
    public void rrect(float x, float y, float w, float h, float r) {
        if (canvas != null && w > 0 && h > 0) {
            rectF.set(x, y, x + w, y + h);
            canvas.drawRoundRect(rectF, Math.max(0, r), Math.max(0, r), fillPaint);
        }
    }

    @Override
    public void rrectStroke(float x, float y, float w, float h, float r, float lw) {
        if (canvas != null && w > 0 && h > 0 && lw > 0) {
            strokePaint.setStrokeWidth(lw);
            rectF.set(x, y, x + w, y + h);
            canvas.drawRoundRect(rectF, Math.max(0, r), Math.max(0, r), strokePaint);
        }
    }

    @Override
    public void oval(float x, float y, float w, float h) {
        if (canvas != null && w > 0 && h > 0) {
            rectF.set(x, y, x + w, y + h);
            canvas.drawOval(rectF, fillPaint);
        }
    }

    @Override
    public void ovalStroke(float x, float y, float w, float h, float lw) {
        if (canvas != null && w > 0 && h > 0 && lw > 0) {
            strokePaint.setStrokeWidth(lw);
            rectF.set(x, y, x + w, y + h);
            canvas.drawOval(rectF, strokePaint);
        }
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float lw) {
        if (canvas != null && lw > 0) {
            strokePaint.setStrokeWidth(lw);
            strokePaint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(x1, y1, x2, y2, strokePaint);
        }
    }

    @Override
    public void poly(float[] xy) {
        if (canvas == null || xy == null || xy.length < 6) return;
        path.reset();
        path.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            path.lineTo(xy[i], xy[i + 1]);
        }
        path.close();
        canvas.drawPath(path, fillPaint);
    }

    @Override
    public void gradient(float x, float y, float w, float h, float r, int c1, int c2, boolean vertical) {
        if (canvas == null || w <= 0.001f || h <= 0.001f) return;
        try {
            float x1 = vertical ? x : x + w;
            float y1 = vertical ? y + h : y;
            if (Math.abs(x - x1) < 0.001f && Math.abs(y - y1) < 0.001f) return;

            LinearGradient grad = new LinearGradient(
                x, y, x1, y1,
                argb(c1), argb(c2), Shader.TileMode.CLAMP
            );
            fillPaint.setShader(grad);
            rectF.set(x, y, x + w, y + h);
            canvas.drawRoundRect(rectF, Math.max(0, r), Math.max(0, r), fillPaint);
            fillPaint.setShader(null);
        } catch (Throwable ignored) {
            fillPaint.setShader(null);
        }
    }

    @Override
    public void radial(float cx, float cy, float r, int inner, int outer) {
        if (canvas == null || r <= 0.001f) return;
        try {
            RadialGradient grad = new RadialGradient(cx, cy, r, argb(inner), argb(outer), Shader.TileMode.CLAMP);
            fillPaint.setShader(grad);
            canvas.drawCircle(cx, cy, r, fillPaint);
            fillPaint.setShader(null);
        } catch (Throwable ignored) {
            fillPaint.setShader(null);
        }
    }

    @Override
    public void text(String s, float x, float y, float size, boolean bold, int align) {
        if (canvas == null || s == null || size <= 0.001f) return;
        try {
            textPaint.setTextSize(size);
            textPaint.setTypeface(bold ? fontBold : fontRegular);
            textPaint.setColor(fillPaint.getColor());

            Paint.Align pa = Paint.Align.LEFT;
            if (align == CENTER) pa = Paint.Align.CENTER;
            else if (align == RIGHT) pa = Paint.Align.RIGHT;
            textPaint.setTextAlign(pa);

            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float baseline = y - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(s, x, baseline, textPaint);
        } catch (Throwable ignored) {}
    }

    @Override
    public float textWidth(String s, float size, boolean bold) {
        if (s == null || size <= 0.001f) return 0;
        try {
            textPaint.setTextSize(size);
            textPaint.setTypeface(bold ? fontBold : fontRegular);
            return textPaint.measureText(s);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @Override
    public void save() {
        if (canvas != null) canvas.save();
    }

    @Override
    public void restore() {
        if (canvas != null) canvas.restore();
    }

    @Override
    public void translate(float x, float y) {
        if (canvas != null) canvas.translate(x, y);
    }

    @Override
    public void rotate(float deg) {
        if (canvas != null) canvas.rotate(deg);
    }

    @Override
    public void scale(float sx, float sy) {
        if (canvas != null) canvas.scale(sx, sy);
    }

    @Override
    public void clip(float x, float y, float w, float h) {
        if (canvas != null) canvas.clipRect(x, y, x + w, y + h);
    }
}
