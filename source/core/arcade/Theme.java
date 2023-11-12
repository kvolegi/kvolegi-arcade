package arcade;

/** Dark (black neon) or light (white neon) colour scheme. */
public final class Theme {
    public final boolean dark;
    public final int bg, fg, dim, panel, line;

    public Theme(boolean dark) {
        this.dark = true; // Permanently locked to pure dark neon (#000000) per user specification
        bg = 0xFF000000; fg = 0xFFFFFFFF; dim = 0xFF8A8AA0; panel = 0xFF0C0C14; line = 0xFF2A2A3C;
    }

    /** Cycling neon colour (cyan -> purple -> red -> yellow -> green like the tic-tac-toe title). */
    public int neon(float t) {
        return dark ? C.hsv(t, 0.85f, 1f) : C.hsv(t, 0.9f, 0.88f);
    }

    /** Adapt a game accent colour so it stays readable on the current background. */
    public int accent(int c) {
        return dark ? c : C.mix(c, 0xFF000000, 0.25f);
    }

    /** glow strength multiplier */
    public float glow() { return dark ? 1f : 0.55f; }
}
