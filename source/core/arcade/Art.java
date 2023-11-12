package arcade;

/** All vector artwork: logo, game icons and sprites. Drawn through {@link Gfx} so it also exports to PNG. */
public final class Art {
    private Art() {}

    private static void circ(Gfx g, float cx, float cy, float r) { g.oval(cx - r, cy - r, 2 * r, 2 * r); }
    private static void ball(Gfx g, float cx, float cy, float r, int base) {
        g.radial(cx - r * 0.25f, cy - r * 0.3f, r * 1.25f, C.mix(base, 0xFFFFFFFF, 0.55f), base);
        // radial() fills a circle of radius r*1.25 -> clip by redrawing with exact radius
    }

    /** Glossy ball with exact radius. */
    public static void gloss(Gfx g, float cx, float cy, float r, int base) {
        g.radial(cx, cy, r, base, C.mix(base, 0xFF000000, 0.35f));
        g.color(C.a(0xFFFFFFFF, 0.45f));
        g.oval(cx - r * 0.55f, cy - r * 0.62f, r * 0.55f, r * 0.34f);
    }

    // ------------------------------------------------------------------ backdrop
    public static void orbs(Gfx g, Ui ui, float w, float h) {
        float t = ui.time * 0.18f;
        float s = Math.max(w, h);
        for (int i = 0; i < 4; i++) {
            float x = w * (0.5f + 0.42f * (float) Math.sin(t * (0.7f + i * 0.23f) + i * 1.9f));
            float y = h * (0.5f + 0.42f * (float) Math.cos(t * (0.6f + i * 0.19f) + i * 2.7f));
            int c = ui.th.neon(i * 0.27f + ui.time * 0.02f);
            g.radial(x, y, s * 0.36f, C.a(c, ui.th.dark ? 0.16f : 0.13f), C.a(c, 0f));
        }
    }

    /** Exact 6-second rainbow cycle from TicTacToeFX (Cyan -> Purple -> Red -> Yellow -> Green -> Lime). */
    public static int cyclingRainbow(float time) {
        int[] cols = {
            0xFF00FFFF, // Cyan (0s)
            0xFFA855F7, // Purple (1s)
            0xFFEF4444, // Red (2s)
            0xFFEAB308, // Yellow (3s)
            0xFF22C55E, // Green (4s)
            0xFF84CC16, // Lime (5s)
            0xFF00FFFF  // Cyan (6s)
        };
        float t = time % 6f;
        if (t < 0) t += 6f;
        int idx = (int) t;
        float frac = t - idx;
        int c1 = cols[idx % cols.length];
        int c2 = cols[(idx + 1) % cols.length];
        return C.mix(c1, c2, frac);
    }

    // ------------------------------------------------------------------ logo & title glow
    /** DropShadow radius 50 glow behind solid black text matching photo 1:1. */
    public static void titleGlow(Gfx g, String text, float cx, float cy, float size, int glowCol) {
        float scale = size / 75f;
        float[] dists = {5.2f * scale, 3.0f * scale, 1.2f * scale};
        float[] alphas = {0.18f, 0.35f, 0.55f};
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        for (int p = 0; p < 3; p++) {
            g.color(C.a(glowCol, alphas[p]));
            float d = dists[p];
            for (int[] dir : dirs) {
                g.text(text, cx + dir[0] * d, cy + dir[1] * d, size, true, Gfx.CENTER);
            }
        }
        g.color(0xFF000000);
        g.text(text, cx, cy, size, true, Gfx.CENTER);
    }

    /**
     * Authentic Logo matching photo: pure black text with smoothly transitioning neon halo,
     * "kvolegi" centered and "arcade" below starting right at the letter 'l' (slightly to the right of center).
     */
    public static void logo(Gfx g, Ui ui, float cx, float cy, float size, boolean glow) {
        float scale = size / 75f;
        float szMain = 76 * scale;
        float szSub = 28 * scale;

        int glowCol = cyclingRainbow(ui.time);

        float wMain = g.textWidth("kvolegi", szMain, true);
        float wKvo = g.textWidth("kvo", szMain, true);

        float mainX = cx;
        float mainY = cy;

        // "arcade" placed lower and shifted right under "kvolegi" per user request
        float leftMain = cx - wMain / 2f;
        float subX = leftMain + wKvo + 24 * scale;
        float subY = cy + szMain * 0.68f;

        if (glow) {
            // Multi-pass radiant DropShadow bloom matching PC outerBloom
            float[] dists = {6.0f * scale, 4.0f * scale, 2.5f * scale, 1.2f * scale};
            float[] alphas = {0.18f, 0.35f, 0.55f, 0.75f};
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
            for (int p = 0; p < dists.length; p++) {
                g.color(C.a(glowCol, alphas[p]));
                float d = dists[p];
                for (int[] dir : dirs) {
                    g.text("kvolegi", mainX + dir[0] * d, mainY + dir[1] * d, szMain, true, Gfx.CENTER);
                    g.text("arcade", subX + dir[0] * d, subY + dir[1] * d, szSub, true, Gfx.LEFT);
                }
            }

            // Crisp colored outline stroke matching PC title.strokeProperty() 1:1
            float strokeD = Math.max(1.2f, 1.8f * scale);
            g.color(glowCol);
            for (int[] dir : dirs) {
                g.text("kvolegi", mainX + dir[0] * strokeD, mainY + dir[1] * strokeD, szMain, true, Gfx.CENTER);
                g.text("arcade", subX + dir[0] * strokeD * 0.8f, subY + dir[1] * strokeD * 0.8f, szSub, true, Gfx.LEFT);
            }
        }

        // Solid pure dark fill on top (#050508 matching PC)
        g.color(0xFF050508);
        g.text("kvolegi", mainX, mainY, szMain, true, Gfx.CENTER);
        g.text("arcade", subX, subY, szSub, true, Gfx.LEFT);
    }

    private static void capsule(Gfx g, float x1, float y1, float x2, float y2, float r, int col) {
        g.color(col);
        circ(g, x1, y1, r);
        circ(g, x2, y2, r);
        float dx = x2 - x1, dy = y2 - y1;
        float d = (float) Math.hypot(dx, dy);
        if (d > 0.001f) {
            float nx = -dy / d * r, ny = dx / d * r;
            g.poly(new float[]{
                x1 + nx, y1 + ny,
                x2 + nx, y2 + ny,
                x2 - nx, y2 - ny,
                x1 - nx, y1 - ny
            });
        }
    }

    private static void leftPill(Gfx g, float x, float y, float c) {
        circ(g, x + c / 2, y + c / 2, c / 2);
        g.rect(x + c / 2, y, c / 2, c);
    }

    /** Rounded-square app tile with the logo; used for launcher icons. */
    public static void appIcon(Gfx g, Ui ui, float cx, float cy, float size) {
        float x = cx - size / 2, y = cy - size / 2;
        g.gradient(x, y, size, size, size * 0.22f, 0xFF14141F, 0xFF000000, true);
        g.color(C.a(C.PURPLE, 0.12f));
        g.rrectStroke(x + size * 0.01f, y + size * 0.01f, size * 0.98f, size * 0.98f, size * 0.21f, size * 0.02f);
        logo(g, ui, cx, cy, size * 0.58f, true);
    }

    // ------------------------------------------------------------------ game icons
    /** Tile background for a game icon, returns nothing; glyph is drawn by the game. */
    public static void tile(Gfx g, Ui ui, float cx, float cy, float s, int accent) {
        float x = cx - s / 2, y = cy - s / 2, r = s * 0.22f;
        ui.glowRR(g, x, y, s, s, r, accent, 1f);
        g.gradient(x, y, s, s, r, ui.th.dark ? C.mix(accent, 0xFF000000, 0.78f) : C.mix(accent, 0xFFFFFFFF, 0.82f),
                ui.th.dark ? 0xFF05050A : C.mix(accent, 0xFFFFFFFF, 0.93f), true);
        g.color(accent);
        g.rrectStroke(x, y, s, s, r, Math.max(1.6f, s * 0.03f));
    }

    public static void snakeIcon(Gfx g, Ui ui, float cx, float cy, float s) {
        tile(g, ui, cx, cy, s, C.GREEN);
        float r = s * 0.1f;
        float[][] p = {{-0.28f, 0.22f}, {-0.1f, 0.22f}, {0.08f, 0.22f}, {0.26f, 0.22f}, {0.26f, 0.02f}, {0.26f, -0.18f}, {0.08f, -0.18f}, {-0.1f, -0.18f}};
        for (int i = 0; i < p.length; i++) {
            float t = i / (float) (p.length - 1);
            g.color(C.mix(C.PURPLE, C.CYAN, t));
            g.rrect(cx + p[i][0] * s - r, cy + p[i][1] * s - r, r * 2, r * 2, r * 0.7f);
        }
        // head with eyes
        float hx = cx - 0.28f * s, hy = cy - 0.18f * s;
        g.color(C.CYAN);
        g.rrect(hx - r * 1.15f, hy - r * 1.15f, r * 2.3f, r * 2.3f, r);
        g.color(0xFF000000); circ(g, hx - r * 0.35f, hy - r * 0.3f, r * 0.28f); circ(g, hx + r * 0.45f, hy - r * 0.3f, r * 0.28f);
        beer(g, cx - 0.28f * s, cy + 0.22f * s, s * 0.10f);
    }

    public static void tttIcon(Gfx g, Ui ui, float cx, float cy, float s) {
        tile(g, ui, cx, cy, s, C.PURPLE);
        float a = s * 0.2f, lw = s * 0.075f;
        g.color(C.CYAN);
        g.line(cx - 0.3f * s, cy - 0.3f * s, cx - 0.3f * s + a, cy - 0.3f * s + a, lw);
        g.line(cx - 0.3f * s + a, cy - 0.3f * s, cx - 0.3f * s, cy - 0.3f * s + a, lw);
        g.color(C.PURPLE);
        g.line(cx + 0.1f * s, cy + 0.1f * s, cx + 0.1f * s + a * 1.2f, cy + 0.1f * s + a * 1.2f, lw);
        g.line(cx + 0.1f * s + a * 1.2f, cy + 0.1f * s, cx + 0.1f * s, cy + 0.1f * s + a * 1.2f, lw);
        g.color(C.RED);
        g.ovalStroke(cx + 0.08f * s, cy - 0.33f * s, a * 1.25f, a * 1.25f, lw);
        g.color(C.BLUE);
        g.ovalStroke(cx - 0.32f * s, cy + 0.08f * s, a * 1.25f, a * 1.25f, lw);
    }

    public static void minesIcon(Gfx g, Ui ui, float cx, float cy, float s) {
        tile(g, ui, cx, cy, s, C.ORANGE);
        beaver(g, cx - s * 0.08f, cy, s * 0.38f);
        broccoli(g, cx + s * 0.28f, cy + s * 0.18f, s * 0.14f);
    }

    public static void slotsIcon(Gfx g, Ui ui, float cx, float cy, float s) {
        tile(g, ui, cx, cy, s, C.YELLOW);
        // Reels frame
        g.color(0xFF141420);
        g.rrect(cx - s * 0.38f, cy - s * 0.28f, s * 0.76f, s * 0.56f, s * 0.06f);
        g.color(0xFF32324A);
        g.rrectStroke(cx - s * 0.38f, cy - s * 0.28f, s * 0.76f, s * 0.56f, s * 0.06f, 1.5f);
        // Reel windows
        g.line(cx - s * 0.12f, cy - s * 0.28f, cx - s * 0.12f, cy + s * 0.28f, 1.5f);
        g.line(cx + s * 0.12f, cy - s * 0.28f, cx + s * 0.12f, cy + s * 0.28f, 1.5f);
        // Golden win line
        g.color(0xFFFFD60A);
        g.line(cx - s * 0.38f, cy, cx + s * 0.38f, cy, 2.5f);
        // Fruits
        fruit(g, 1, cx - s * 0.25f, cy, s * 0.14f); // Watermelon
        fruit(g, 7, cx, cy, s * 0.16f);             // Golden Wild Banana
        fruit(g, 0, cx + s * 0.25f, cy, s * 0.14f); // Grapes
    }

    // ------------------------------------------------------------------ sprites
    public static void apple(Gfx g, float cx, float cy, float r) {
        g.radial(cx, cy + r * 0.1f, r * 0.9f, 0xFFFF7A88, 0xFFD0123A);
        g.color(C.a(0xFFFFFFFF, 0.5f)); g.oval(cx - r * 0.5f, cy - r * 0.55f, r * 0.4f, r * 0.28f);
        g.color(0xFF6B3A1E); g.line(cx, cy - r * 0.7f, cx + r * 0.12f, cy - r * 1.1f, Math.max(1.5f, r * 0.14f));
        g.color(0xFF39D96B);
        g.poly(new float[]{cx + r * 0.12f, cy - r * 0.95f, cx + r * 0.75f, cy - r * 1.15f, cx + r * 0.45f, cy - r * 0.65f});
    }

    public static void broccoli(Gfx g, float cx, float cy, float r) {
        g.gradient(cx - r * 0.2f, cy + r * 0.05f, r * 0.4f, r * 0.9f, r * 0.15f, 0xFFA8E06A, 0xFF6DB33A, true);
        gloss(g, cx - r * 0.48f, cy - r * 0.1f, r * 0.45f, 0xFF2FB24A);
        gloss(g, cx + r * 0.48f, cy - r * 0.1f, r * 0.45f, 0xFF2FB24A);
        gloss(g, cx, cy - r * 0.45f, r * 0.55f, 0xFF3CCB5A);
        gloss(g, cx, cy + r * 0.05f, r * 0.45f, 0xFF27A043);
    }

    public static void beer(Gfx g, float cx, float cy, float r) {
        g.color(0xFFFFE9A8);
        g.ovalStroke(cx + r * 0.38f, cy - r * 0.3f, r * 0.62f, r * 0.75f, r * 0.14f);
        g.gradient(cx - r * 0.65f, cy - r * 0.55f, r * 1.1f, r * 1.5f, r * 0.14f, 0xFFFFD04A, 0xFFE08A00, true);
        g.color(C.a(0xFFFFFFFF, 0.45f)); g.rrect(cx - r * 0.5f, cy - r * 0.35f, r * 0.14f, r * 1.0f, r * 0.07f);
        g.color(0xFFFFFFFF);
        circ(g, cx - r * 0.45f, cy - r * 0.6f, r * 0.3f); circ(g, cx - r * 0.1f, cy - r * 0.75f, r * 0.34f); circ(g, cx + r * 0.25f, cy - r * 0.55f, r * 0.28f);
    }

    /** Full-body standing beaver ("в полный рост бобровый"). */
    public static void beaver(Gfx g, float cx, float cy, float r) {
        // Flat beaver paddle tail extending to the lower right
        g.color(0xFF381F12);
        g.oval(cx + r * 0.15f, cy + r * 0.25f, r * 0.70f, r * 0.45f);
        g.color(0xFF221108);
        g.line(cx + r * 0.28f, cy + r * 0.32f, cx + r * 0.65f, cy + r * 0.60f, 1.2f);
        g.line(cx + r * 0.40f, cy + r * 0.30f, cx + r * 0.75f, cy + r * 0.52f, 1.2f);
        g.line(cx + r * 0.65f, cy + r * 0.32f, cx + r * 0.28f, cy + r * 0.60f, 1.2f);

        // Little back feet at bottom
        g.color(0xFF4A2A18);
        g.oval(cx - r * 0.38f, cy + r * 0.65f, r * 0.34f, r * 0.20f);
        g.oval(cx + r * 0.04f, cy + r * 0.65f, r * 0.34f, r * 0.20f);

        // Body / Torso (standing upright)
        gloss(g, cx - r * 0.04f, cy + r * 0.22f, r * 0.54f, 0xFF7A4429);
        // Light tummy
        g.radial(cx - r * 0.04f, cy + r * 0.26f, r * 0.34f, 0xFFE8C89A, 0xFFC99565);

        // Small round ears
        gloss(g, cx - r * 0.36f, cy - r * 0.46f, r * 0.16f, 0xFF6B361B);
        gloss(g, cx + r * 0.26f, cy - r * 0.46f, r * 0.16f, 0xFF6B361B);

        // Head
        gloss(g, cx - r * 0.04f, cy - r * 0.16f, r * 0.46f, 0xFF8A5236);

        // Cheeks / muzzle
        g.radial(cx - r * 0.04f, cy - r * 0.04f, r * 0.26f, 0xFFF5D6AE, 0xFFD9A978);

        // Black eyes with catchlights
        g.color(0xFF1A0E08);
        circ(g, cx - r * 0.20f, cy - r * 0.18f, r * 0.065f);
        circ(g, cx + r * 0.12f, cy - r * 0.18f, r * 0.065f);
        g.color(0xFFFFFFFF);
        circ(g, cx - r * 0.22f, cy - r * 0.20f, r * 0.022f);
        circ(g, cx + r * 0.10f, cy - r * 0.20f, r * 0.022f);

        // Nose
        g.color(0xFF221108);
        g.oval(cx - r * 0.11f, cy - r * 0.11f, r * 0.14f, r * 0.09f);

        // Big white buck teeth!
        g.color(0xFFFFFFFF);
        g.rrect(cx - r * 0.10f, cy - r * 0.01f, r * 0.09f, r * 0.15f, 1.5f);
        g.rrect(cx - r * 0.00f, cy - r * 0.01f, r * 0.09f, r * 0.15f, 1.5f);
        g.color(0xFF221108);
        g.line(cx - r * 0.00f, cy - r * 0.01f, cx - r * 0.00f, cy + r * 0.14f, 1f);

        // Little front paws resting on the belly
        g.color(0xFF5A301A);
        g.oval(cx - r * 0.26f, cy + r * 0.14f, r * 0.18f, r * 0.12f);
        g.oval(cx + r * 0.08f, cy + r * 0.14f, r * 0.18f, r * 0.12f);
    }

    public static void cabbage(Gfx g, float cx, float cy, float r) {
        g.radial(cx, cy, r, 0xFF3FA84A, 0xFF1F6B2E);
        g.radial(cx, cy, r * 0.78f, 0xFFB6EE8A, 0xFF5FC25A);
        g.radial(cx + r * 0.05f, cy + r * 0.05f, r * 0.5f, 0xFFE4F9C4, 0xFF9AD66A);
        g.color(C.a(0xFF1F6B2E, 0.55f));
        float lw = Math.max(1.2f, r * 0.05f);
        g.line(cx - r * 0.55f, cy - r * 0.2f, cx - r * 0.1f, cy + r * 0.1f, lw);
        g.line(cx + r * 0.6f, cy - r * 0.15f, cx + r * 0.12f, cy + r * 0.12f, lw);
        g.line(cx, cy + r * 0.12f, cx, cy + r * 0.7f, lw);
    }

    public static void fruit(Gfx g, int idx, float cx, float cy, float r) {
        switch (idx) {
            case 0: { // grapes
                int gc = 0xFF8E44E0;
                float br = r * 0.3f;
                g.color(0xFF6B3A1E); g.line(cx, cy - r * 0.8f, cx + r * 0.1f, cy - r * 1.1f, Math.max(1.5f, r * 0.1f));
                g.color(0xFF39D96B); g.poly(new float[]{cx + r * 0.1f, cy - r * 0.95f, cx + r * 0.7f, cy - r * 1.1f, cx + r * 0.4f, cy - r * 0.7f});
                for (int row = 0; row < 3; row++) for (int k = 0; k <= 2 - row; k++)
                    gloss(g, cx + (k - (2 - row) / 2f) * br * 2.0f, cy - r * 0.35f + row * br * 1.75f, br, gc);
                gloss(g, cx, cy + r * 0.95f, br, gc);
                break; }
            case 1: { // watermelon slice
                int n = 20; float[] pr = new float[(n + 1) * 2], pg = new float[(n + 1) * 2];
                for (int i = 0; i <= n; i++) {
                    double a = Math.PI * i / n;
                    pr[i * 2] = cx + (float) Math.cos(a) * r * 1.15f; pr[i * 2 + 1] = cy - r * 0.55f + (float) Math.sin(a) * r * 1.15f;
                    pg[i * 2] = cx + (float) Math.cos(a) * r * 0.95f; pg[i * 2 + 1] = cy - r * 0.55f + (float) Math.sin(a) * r * 0.95f;
                }
                g.color(0xFF1F9A3E); g.poly(pr);
                g.color(0xFFFF4D5E); g.poly(pg);
                g.color(0xFF2A1218);
                for (int i = 0; i < 5; i++) { double a = Math.PI * (i + 1) / 6; 
                    g.save(); g.translate(cx + (float) Math.cos(a) * r * 0.55f, cy - r * 0.55f + (float) Math.sin(a) * r * 0.55f); g.rotate(-20);
                    g.oval(-r * 0.06f, -r * 0.1f, r * 0.12f, r * 0.2f); g.restore(); }
                break; }
            case 2: { // lemon
                g.save(); g.translate(cx, cy); g.rotate(-30);
                g.radial(0, 0, r * 0.7f, 0xFFFFF27A, 0xFFE8C400);
                g.gradient(-r * 0.95f, -r * 0.62f, r * 1.9f, r * 1.24f, r * 0.62f, 0xFFFFF27A, 0xFFE8B800, true);
                g.color(0xFFE8B800); circ(g, -r * 0.98f, 0, r * 0.14f); circ(g, r * 0.98f, 0, r * 0.14f);
                g.color(C.a(0xFFFFFFFF, 0.5f)); g.oval(-r * 0.5f, -r * 0.5f, r * 0.7f, r * 0.2f);
                g.restore();
                break; }
            case 3: { // pineapple
                g.color(0xFF2FB24A);
                g.poly(new float[]{cx, cy - r * 0.45f, cx - r * 0.55f, cy - r * 1.15f, cx - r * 0.15f, cy - r * 0.7f});
                g.poly(new float[]{cx, cy - r * 0.45f, cx + r * 0.55f, cy - r * 1.15f, cx + r * 0.15f, cy - r * 0.7f});
                g.color(0xFF1F8A38);
                g.poly(new float[]{cx - r * 0.18f, cy - r * 0.45f, cx, cy - r * 1.3f, cx + r * 0.18f, cy - r * 0.45f});
                g.gradient(cx - r * 0.62f, cy - r * 0.5f, r * 1.24f, r * 1.55f, r * 0.55f, 0xFFFFC23A, 0xFFE07A00, true);
                g.color(C.a(0xFF9A4A00, 0.7f));
                float lw = Math.max(1f, r * 0.05f);
                for (int i = -2; i <= 2; i++) {
                    g.line(cx + i * r * 0.3f - r * 0.4f, cy - r * 0.3f, cx + i * r * 0.3f + r * 0.4f, cy + r * 0.9f, lw);
                    g.line(cx + i * r * 0.3f + r * 0.4f, cy - r * 0.3f, cx + i * r * 0.3f - r * 0.4f, cy + r * 0.9f, lw);
                }
                break; }
            case 4: { // plum
                g.color(0xFF6B3A1E); g.line(cx, cy - r * 0.7f, cx + r * 0.05f, cy - r * 1.1f, Math.max(1.5f, r * 0.1f));
                g.color(0xFF39D96B); g.poly(new float[]{cx + r * 0.05f, cy - r * 0.95f, cx + r * 0.7f, cy - r * 1.1f, cx + r * 0.4f, cy - r * 0.7f});
                gloss(g, cx, cy + r * 0.1f, r * 0.9f, 0xFF5B4BE0);
                g.color(C.a(0xFF2A1F7A, 0.5f)); g.line(cx, cy - r * 0.6f, cx, cy + r * 0.2f, Math.max(1f, r * 0.05f));
                break; }
            case 5: { // df = dragonfruit from original SigmaSlots
                // Magenta-pink body with green outer leafy tips
                g.color(0xFF22C55E); // green bracts/tips
                g.poly(new float[]{cx - r * 0.85f, cy - r * 0.35f, cx - r * 0.35f, cy - r * 0.95f, cx, cy - r * 0.45f});
                g.poly(new float[]{cx + r * 0.85f, cy - r * 0.35f, cx + r * 0.35f, cy - r * 0.95f, cx, cy - r * 0.45f});
                g.poly(new float[]{cx - r * 0.2f, cy - r * 1.15f, cx + r * 0.2f, cy - r * 1.15f, cx, cy - r * 0.65f});
                // Outer pitaya magenta skin
                gloss(g, cx, cy + r * 0.05f, r * 0.88f, 0xFFE11D48);
                // Inner white fleshy cross-section
                g.radial(cx, cy + r * 0.08f, r * 0.65f, 0xFFFFFFFF, 0xFFF1F5F9);
                // Tiny black seeds
                g.color(0xFF1E293B);
                for (int s = 0; s < 12; s++) {
                    double a = s * 0.52 + 0.2;
                    float sr = r * (0.22f + 0.30f * ((s % 3) / 2.0f));
                    circ(g, cx + (float) Math.cos(a) * sr, cy + r * 0.08f + (float) Math.sin(a) * sr, Math.max(1.0f, r * 0.045f));
                }
                break; }
            case 6: banana(g, cx, cy, r, 0xFFFFE04A, 0xFFE0A800); break;
            case 7: { // golden banana = WILD
                g.radial(cx, cy, r * 1.3f, C.a(0xFFFFE066, 0.45f), C.a(0xFFFFE066, 0f));
                banana(g, cx - r * 0.12f, cy + r * 0.05f, r * 1.1f, 0xFFFFF0A0, 0xFFE0A800);
                banana(g, cx + r * 0.18f, cy - r * 0.05f, r * 1.1f, 0xFFFFD21F, 0xFFC98A00);
                g.color(0xFF3A2400); g.text("WILD", cx, cy + r * 0.95f, r * 0.55f, true, Gfx.CENTER);
                break; }
            default: { // scatter / BIG WIN star
                g.radial(cx, cy, r * 1.3f, C.a(0xFFFF4FD8, 0.5f), C.a(0xFFFF4FD8, 0f));
                float[] pts = new float[20];
                for (int i = 0; i < 10; i++) { double a = -Math.PI / 2 + i * Math.PI / 5; float rr = (i % 2 == 0 ? 1.05f : 0.5f) * r;
                    pts[i * 2] = cx + (float) Math.cos(a) * rr; pts[i * 2 + 1] = cy + (float) Math.sin(a) * rr; }
                g.color(0xFFFF4FD8); g.poly(pts);
                float[] in = new float[20];
                for (int i = 0; i < 20; i += 2) { in[i] = cx + (pts[i] - cx) * 0.8f; in[i + 1] = cy + (pts[i + 1] - cy) * 0.8f; }
                g.color(0xFFFFD21F); g.poly(in);
                g.color(0xFF5A1A6A); g.text("BIG", cx, cy - r * 0.05f, r * 0.5f, true, Gfx.CENTER); g.text("WIN", cx, cy + r * 0.38f, r * 0.42f, true, Gfx.CENTER);
            }
        }
    }

    private static void banana(Gfx g, float cx, float cy, float r, int c1, int c2) {
        int n = 14; float lw = r * 0.46f;
        float px = 0, py = 0;
        for (int i = 0; i <= n; i++) {
            double a = Math.toRadians(200 + i * (140f / n));
            float x = cx + (float) Math.cos(a) * r * 1.0f, y = cy - r * 0.55f - (float) Math.sin(a) * r * 1.0f * -1f;
            y = cy - r * 0.2f + (float) Math.sin(a) * r * 1.0f * -1;
            float t = i / (float) n, th = lw * (0.45f + 0.55f * (float) Math.sin(Math.PI * t));
            if (i > 0) { g.color(C.mix(c1, c2, 1 - t * 0.4f)); g.line(px, py, x, y, th); }
            px = x; py = y;
        }
        g.color(0xFF5A3A12); g.line(cx - r * 0.95f, cy + r * 0.15f, cx - r * 0.85f, cy + r * 0.28f, lw * 0.3f);
    }
}
