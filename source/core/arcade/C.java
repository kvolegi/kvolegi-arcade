package arcade;

/** Colour + math helpers. */
public final class C {
    private C() {}

    public static int rgb(int r, int g, int b) { return 0xFF000000 | (r << 16) | (g << 8) | b; }

    public static int a(int c, float alpha) {
        int al = Math.max(0, Math.min(255, Math.round(((c >>> 24) / 255f) * alpha * 255)));
        return (al << 24) | (c & 0xFFFFFF);
    }

    public static int mix(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a = (int) (((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float c = v * s, x = c * (1 - Math.abs((h * 6) % 2 - 1)), m = v - c;
        float r, g, b;
        int i = (int) (h * 6);
        switch (i) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        return rgb(Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255));
    }

    public static float clamp(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    public static float ease(float t) { t = clamp(t, 0, 1); return t * t * (3 - 2 * t); }
    public static float easeOut(float t) { t = clamp(t, 0, 1); return 1 - (1 - t) * (1 - t) * (1 - t); }

    // neon palette (shared by all games, taken from the tic-tac-toe look)
    public static final int CYAN = 0xFF00E5FF, PURPLE = 0xFFB44CFF, RED = 0xFFFF2E5B, YELLOW = 0xFFFFD60A,
            GREEN = 0xFF39FF88, BLUE = 0xFF4D6BFF, ORANGE = 0xFFFF8A00, PINK = 0xFFFF4FD8;
}
