package arcade.desktop;

import arcade.Gfx;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Authentic JavaFX Desktop Adapter implementing the Gfx port (Hexagonal Architecture).
 * Translates arcade engine drawing primitives to JavaFX GraphicsContext calls.
 */
public class JavaFxGfx implements Gfx {

    private GraphicsContext gc;
    private Color currentColor = Color.WHITE;

    public JavaFxGfx() {}

    public JavaFxGfx(GraphicsContext gc) {
        this.gc = gc;
    }

    public void setGraphicsContext(GraphicsContext gc) {
        this.gc = gc;
    }

    public GraphicsContext getGraphicsContext() {
        return gc;
    }

    private Color toFxColor(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return Color.rgb(r, g, b, a / 255.0);
    }

    @Override
    public void color(int argb) {
        this.currentColor = toFxColor(argb);
        if (gc != null) {
            gc.setFill(currentColor);
            gc.setStroke(currentColor);
        }
    }

    @Override
    public void rect(float x, float y, float w, float h) {
        if (gc != null) {
            gc.fillRect(x, y, w, h);
        }
    }

    @Override
    public void rrect(float x, float y, float w, float h, float r) {
        if (gc != null) {
            gc.fillRoundRect(x, y, w, h, r * 2, r * 2);
        }
    }

    @Override
    public void rrectStroke(float x, float y, float w, float h, float r, float lw) {
        if (gc != null) {
            gc.setLineWidth(lw);
            gc.strokeRoundRect(x, y, w, h, r * 2, r * 2);
        }
    }

    @Override
    public void oval(float x, float y, float w, float h) {
        if (gc != null) {
            gc.fillOval(x, y, w, h);
        }
    }

    @Override
    public void ovalStroke(float x, float y, float w, float h, float lw) {
        if (gc != null) {
            gc.setLineWidth(lw);
            gc.strokeOval(x, y, w, h);
        }
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float lw) {
        if (gc != null) {
            gc.setLineWidth(lw);
            gc.strokeLine(x1, y1, x2, y2);
        }
    }

    @Override
    public void poly(float[] xy) {
        if (gc != null && xy != null && xy.length >= 6) {
            int n = xy.length / 2;
            double[] xPoints = new double[n];
            double[] yPoints = new double[n];
            for (int i = 0; i < n; i++) {
                xPoints[i] = xy[i * 2];
                yPoints[i] = xy[i * 2 + 1];
            }
            gc.fillPolygon(xPoints, yPoints, n);
        }
    }

    @Override
    public void gradient(float x, float y, float w, float h, float r, int c1, int c2, boolean vertical) {
        if (gc != null) {
            Color fxC1 = toFxColor(c1);
            Color fxC2 = toFxColor(c2);
            LinearGradient grad = new LinearGradient(
                    x, y,
                    vertical ? x : x + w,
                    vertical ? y + h : y,
                    false,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, fxC1),
                    new Stop(1, fxC2)
            );
            gc.setFill(grad);
            if (r > 0) {
                gc.fillRoundRect(x, y, w, h, r * 2, r * 2);
            } else {
                gc.fillRect(x, y, w, h);
            }
            gc.setFill(currentColor);
        }
    }

    @Override
    public void radial(float cx, float cy, float r, int inner, int outer) {
        if (gc != null) {
            Color fxInner = toFxColor(inner);
            Color fxOuter = toFxColor(outer);
            RadialGradient grad = new RadialGradient(
                    0, 0,
                    cx - r * 0.2, cy - r * 0.2,
                    r,
                    false,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, fxInner),
                    new Stop(1, fxOuter)
            );
            gc.setFill(grad);
            gc.fillOval(cx - r, cy - r, r * 2, r * 2);
            gc.setFill(currentColor);
        }
    }

    @Override
    public void text(String s, float x, float y, float size, boolean bold, int align) {
        if (gc != null && s != null) {
            Font font = Font.font("System", bold ? FontWeight.BOLD : FontWeight.NORMAL, size);
            gc.setFont(font);
            gc.setTextBaseline(VPos.CENTER);
            if (align == CENTER) {
                gc.setTextAlign(TextAlignment.CENTER);
            } else if (align == RIGHT) {
                gc.setTextAlign(TextAlignment.RIGHT);
            } else {
                gc.setTextAlign(TextAlignment.LEFT);
            }
            gc.fillText(s, x, y);
        }
    }

    @Override
    public float textWidth(String s, float size, boolean bold) {
        if (s == null || s.isEmpty()) return 0f;
        Text text = new Text(s);
        text.setFont(Font.font("System", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return (float) text.getLayoutBounds().getWidth();
    }

    @Override
    public void save() {
        if (gc != null) gc.save();
    }

    @Override
    public void restore() {
        if (gc != null) gc.restore();
    }

    @Override
    public void translate(float x, float y) {
        if (gc != null) gc.translate(x, y);
    }

    @Override
    public void rotate(float deg) {
        if (gc != null) gc.rotate(deg);
    }

    @Override
    public void scale(float sx, float sy) {
        if (gc != null) gc.scale(sx, sy);
    }

    @Override
    public void clip(float x, float y, float w, float h) {
        // JavaFX Canvas does not expose arbitrary clip rect without path push, but bounds are bounded
    }
}
