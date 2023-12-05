package arcade;

/** Minimal 2D drawing surface implemented by JavaFX (desktop) and android.graphics (Android). Units are dp. */
public interface Gfx {
    int LEFT = 0, CENTER = 1, RIGHT = 2;

    void color(int argb);
    void rect(float x, float y, float w, float h);
    void rrect(float x, float y, float w, float h, float r);
    void rrectStroke(float x, float y, float w, float h, float r, float lw);
    void oval(float x, float y, float w, float h);
    void ovalStroke(float x, float y, float w, float h, float lw);
    void line(float x1, float y1, float x2, float y2, float lw);
    void poly(float[] xy);
    /** Fill rounded rect with 2-colour linear gradient. */
    void gradient(float x, float y, float w, float h, float r, int c1, int c2, boolean vertical);
    /** Fill a circle with a radial gradient; highlight is offset to the upper left for a glossy look. */
    void radial(float cx, float cy, float r, int inner, int outer);
    /** y is the vertical centre of the text. */
    void text(String s, float x, float y, float size, boolean bold, int align);
    float textWidth(String s, float size, boolean bold);
    void save();
    void restore();
    void translate(float x, float y);
    void rotate(float deg);
    void scale(float sx, float sy);
    void clip(float x, float y, float w, float h);
}
