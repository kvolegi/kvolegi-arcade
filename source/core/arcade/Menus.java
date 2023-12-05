package arcade;

/** Authentic TicTacToe-inspired menus: Arcade launcher, separate TicTacToe menu & settings, global audio/theme settings. */
final class Menus {
    private Menus() {}

    // ------------------------------------------------------------------ 1. Arcade Launcher Start Screen
    static void main(App app, Gfx g, float w, float h) {
        Ui ui = app.ui;
        Theme th = ui.th;

        // Proportional scale factor matching PC 1050x750 reference
        float scale = h / 750f;

        // 100% synchronized rainbow cycling neon glow matching PC GlowHelper
        int glowCol = Art.cyclingRainbow(ui.time);

        // Top-right corner action buttons (Gear = Settings, Exit = Quit) matching PC ArcadeHub 1:1
        float iconBtnSz = Math.round(52 * scale);
        float padY = Math.round(18 * scale);
        float padX = Math.round(24 * scale);
        if (ui.iconButton(g, Ui.EXIT, w - iconBtnSz - padX, padY, iconBtnSz, glowCol)) {
            app.quitRequested = true;
        }
        if (ui.iconButton(g, Ui.GEAR, w - 2 * iconBtnSz - padX - Math.round(14 * scale), padY, iconBtnSz, glowCol)) {
            app.go(App.Screen.SETTINGS, null);
        }

        // Header Logo & Wide Buttons
        float logoSize = Math.round(Math.min(84 * scale, h * 0.13f));
        float szMain = 76 * (logoSize / 75f);
        float szSub = 28 * (logoSize / 75f);
        float logoBottomRel = szMain * 0.68f + szSub * 0.60f;

        float logoY = Math.round(Math.max(52 * scale, h * 0.125f));
        Art.logo(g, ui, w / 2, logoY, logoSize, true);

        float btnW = Math.round(Math.min(w * 0.54f, Math.max(w * 0.46f, 500 * scale)));
        float btnH = Math.round(Math.min(84 * scale, h * 0.115f));
        float gap = Math.round(Math.min(22 * scale, h * 0.032f));
        float startY = Math.round(logoY + logoBottomRel + 36 * scale);

        // 1. Tic Tac Toe (opens dedicated TicTacToe menu)
        if (drawNeonRectButton(g, ui, "Tic Tac Toe", (w - btnW) / 2, startY, btnW, btnH, glowCol)) {
            app.current = app.games.get(0);
            app.go(App.Screen.TTT_MENU, app.current);
        }

        // 2. Beer Snake
        if (drawNeonRectButton(g, ui, "Beer Snake", (w - btnW) / 2, startY + (btnH + gap), btnW, btnH, glowCol)) {
            app.current = app.games.get(1);
            app.go(App.Screen.PLAY, app.current);
        }

        // 3. Sigma Slots
        if (drawNeonRectButton(g, ui, "Sigma Slots", (w - btnW) / 2, startY + 2 * (btnH + gap), btnW, btnH, glowCol)) {
            app.current = app.games.get(3);
            app.go(App.Screen.PLAY, app.current);
        }

        // 4. Minesweeper
        if (drawNeonRectButton(g, ui, "Minesweeper", (w - btnW) / 2, startY + 3 * (btnH + gap), btnW, btnH, glowCol)) {
            app.current = app.games.get(2);
            app.go(App.Screen.PLAY, app.current);
        }

        // Footer: strictly @kvolegi
        ui.authorTag(g, Math.round(24 * scale), h - Math.round(18 * scale), Gfx.LEFT);
    }

    // ------------------------------------------------------------------ 2. Separate TicTacToe Menu
    static void tttMenu(App app, Gfx g, float w, float h) {
        Ui ui = app.ui;
        Theme th = ui.th;

        float scale = h / 750f;
        int glowCol = Art.cyclingRainbow(ui.time);

        // Header Title: TicTacToe! with authentic glow matching photo
        float titleSize = Math.round(84 * scale);
        float titleY = Math.round(145 * scale);

        Art.titleGlow(g, "TicTacToe!", w / 2, titleY, titleSize, glowCol);

        float btnW = Math.round(Math.min(w * 0.52f, Math.max(w * 0.46f, 500 * scale)));
        float btnH = Math.round(Math.min(88 * scale, h * 0.12f));
        float gap = Math.round(Math.min(26 * scale, h * 0.038f));
        float startY = titleY + Math.round(85 * scale);

        // 1. Start Game
        if (drawNeonRectButton(g, ui, "Start Game", (w - btnW) / 2, startY, btnW, btnH, glowCol)) {
            app.current = app.games.get(0);
            app.go(App.Screen.PLAY, app.current);
        }

        // 2. Settings (dedicated TicTacToe settings)
        if (drawNeonRectButton(g, ui, "Settings", (w - btnW) / 2, startY + (btnH + gap), btnW, btnH, glowCol)) {
            app.go(App.Screen.TTT_SETTINGS, app.games.get(0));
        }

        // 3. Back to Arcade (Exit button removed per user request)
        if (drawNeonRectButton(g, ui, "Arcade Hub", (w - btnW) / 2, startY + 2 * (btnH + gap), btnW, btnH, glowCol)) {
            app.go(App.Screen.MAIN, null);
        }

        // Footer: strictly @kvolegi
        ui.authorTag(g, Math.round(24 * scale), h - Math.round(18 * scale), Gfx.LEFT);
    }

    // ------------------------------------------------------------------ 3. Dedicated TicTacToe Settings
    static void tttSettings(App app, Gfx g, float w, float h) {
        Ui ui = app.ui;
        Theme th = ui.th;

        TicTacToe ttt = (TicTacToe) app.games.get(0);
        float scale = h / 750f;
        int glowCol = Art.cyclingRainbow(ui.time);

        float titleY = Math.round(52 * scale);
        ui.title(g, "Settings", w / 2, titleY, Math.round(44 * scale), glowCol);

        float cw = Math.round(Math.min(w - 60, Math.max(w * 0.52f, 600 * scale)));
        float x0 = (w - cw) / 2;
        float y = titleY + Math.round(48 * scale);

        // Opponent: Human vs Computer
        ui.label(g, "Choose Opponent:", x0 + 4, y, Math.round(17 * scale), false, Gfx.LEFT, th.fg, cw);
        y += Math.round(24 * scale);
        float halfW = (cw - 12) / 2;
        float chipH = Math.round(48 * scale);
        boolean isComp = ttt.oPlayers.sel == 0;
        if (ui.chip(g, "Human", x0, y, halfW, chipH, !isComp, glowCol)) {
            ttt.oPlayers.sel = 1; ttt.saveOpts();
        }
        if (ui.chip(g, "Computer", x0 + halfW + 12, y, halfW, chipH, isComp, glowCol)) {
            ttt.oPlayers.sel = 0; ttt.saveOpts();
        }
        y += Math.round(64 * scale);

        // Play as (X vs O) - if Computer
        if (isComp) {
            ui.label(g, "Play as:", x0 + 4, y, Math.round(17 * scale), false, Gfx.LEFT, th.fg, cw);
            y += Math.round(24 * scale);
            boolean isX = ttt.oSide.sel == 0;
            if (ui.chip(g, "X", x0, y, halfW, chipH, isX, glowCol)) {
                ttt.oSide.sel = 0; ttt.saveOpts();
            }
            if (ui.chip(g, "O", x0 + halfW + 12, y, halfW, chipH, !isX, glowCol)) {
                ttt.oSide.sel = 1; ttt.saveOpts();
            }
            y += Math.round(64 * scale);

            // AI Difficulty
            ui.label(g, "AI Difficulty:", x0 + 4, y, Math.round(17 * scale), false, Gfx.LEFT, th.fg, cw);
            y += Math.round(24 * scale);
            float quartW = (cw - 3 * 8) / 4;
            String[] diffs = {"Easy", "Normal", "Hard", "Nightmare"};
            for (int k = 0; k < 4; k++) {
                if (ui.chip(g, diffs[k], x0 + k * (quartW + 8), y, quartW, Math.round(46 * scale), ttt.oLevel.sel == k, glowCol)) {
                    ttt.oLevel.sel = k; ttt.saveOpts();
                }
            }
            y += Math.round(64 * scale);
        }

        // Game Mode: Line Mode vs Score Mode (Endless board is standard)
        ui.label(g, "Choose Game Mode:", x0 + 4, y, Math.round(17 * scale), false, Gfx.LEFT, th.fg, cw);
        y += Math.round(24 * scale);
        boolean isLine = ttt.oMode.sel == 0;
        if (ui.chip(g, "Line Mode (Endless)", x0, y, halfW, chipH, isLine, glowCol)) {
            ttt.oMode.sel = 0; ttt.saveOpts();
        }
        if (ui.chip(g, "Score Mode (60s)", x0 + halfW + 12, y, halfW, chipH, !isLine, glowCol)) {
            ttt.oMode.sel = 1; ttt.saveOpts();
        }
        y += Math.round(72 * scale);

        // Winning line length: strictly locked to 5 in a row per user request
        ttt.oWin.sel = 1; ttt.saveOpts();

        // Save Settings (matches photo with synchronized neon glow)
        float saveW = Math.round(360 * scale);
        if (drawNeonRectButton(g, ui, "Save Settings", (w - saveW) / 2, y, saveW, Math.round(62 * scale), glowCol)) {
            app.go(App.Screen.TTT_MENU, ttt);
        }

        ui.authorTag(g, Math.round(24 * scale), h - Math.round(18 * scale), Gfx.LEFT);
    }

    // ------------------------------------------------------------------ 4. Global Settings (Sound, Volume)
    static void settings(App app, Gfx g, float w, float h) {
        Ui ui = app.ui;
        Theme th = ui.th;
        float scale = h / 750f;
        int glowCol = Art.cyclingRainbow(ui.time);

        float titleY = Math.round(48 * scale);
        ui.title(g, "Settings", w / 2, titleY, Math.round(44 * scale), glowCol);

        float cw = Math.round(Math.min(w - 60, Math.max(w * 0.48f, 540 * scale)));
        float x0 = (w - cw) / 2;
        float y = titleY + Math.round(48 * scale);
        float halfW = (cw - 12) / 2;
        float chipH = Math.round(44 * scale);

        // 1. Sound Effects
        ui.label(g, "Sound Effects:", x0 + 4, y, Math.round(18 * scale), false, Gfx.LEFT, th.fg, cw);
        y += Math.round(26 * scale);
        if (ui.chip(g, "Sound ON", x0, y, halfW, chipH, app.soundEnabled, glowCol)) {
            app.setSound(true);
        }
        if (ui.chip(g, "Sound OFF", x0 + halfW + 12, y, halfW, chipH, !app.soundEnabled, C.RED)) {
            app.setSound(false);
        }
        y += Math.round(72 * scale);

        // 2. Music Volume
        ui.label(g, "Music Volume: " + (app.musicVolume == 0 ? "Mute" : app.musicVolume + "%"), x0 + 4, y, Math.round(18 * scale), false, Gfx.LEFT, th.fg, cw);
        y += Math.round(26 * scale);
        float volChipW = (cw - 3 * 8) / 4;
        int[] vols = {0, 30, 70, 100};
        String[] volLabels = {"Mute", "30%", "70%", "100%"};
        for (int k = 0; k < 4; k++) {
            boolean sel = app.musicVolume == vols[k];
            if (ui.chip(g, volLabels[k], x0 + k * (volChipW + 8), y, volChipW, Math.round(44 * scale), sel, glowCol)) {
                app.setVolume(vols[k]);
            }
        }
        y += Math.round(80 * scale);

        // Back to Arcade button
        float btnW = Math.round(Math.min(360 * scale, cw));
        if (drawNeonRectButton(g, ui, "Back to Arcade", (w - btnW) / 2, y, btnW, Math.round(58 * scale), glowCol)) {
            app.go(App.Screen.MAIN, null);
        }

        // Footer: strictly @kvolegi
        ui.authorTag(g, Math.round(24 * scale), h - Math.round(18 * scale), Gfx.LEFT);
    }

    // ------------------------------------------------------------------ 5. Money Modal: ":("
    static void moneyModal(App app, Gfx g, float w, float h) {
        Ui ui = app.ui;
        Theme th = ui.th;
        float scale = h / 750f;

        // Dark dim backdrop
        g.color(0xDC000000);
        g.rect(0, 0, w, h);

        float mw = Math.round(Math.min(w - 30, Math.max(w * 0.52f, 560 * scale)));
        float mh = Math.round(Math.min(h - 30, 490 * scale));
        float mx = (w - mw) / 2;
        float my = (h - mh) / 2;
        float r = 4.0f;

        // Neon Yellow Glow & Black Box
        ui.dropShadowGlow(g, mx, my, mw, mh, r, C.YELLOW, 1.4f);
        g.color(0xFF000000);
        g.rrect(mx, my, mw, mh, r);
        g.color(0xFFFFFFFF);
        g.rrectStroke(mx, my, mw, mh, r, 2.5f);

        // Header Title
        ui.title(g, ":(", mx + mw / 2, my + 38 * scale, Math.min(38 * scale, mw * 0.10f), C.YELLOW);

        // Subtitle / current balance
        int curBal = app.store.getInt("slots.bal", 1000);
        ui.label(g, "Баланс: " + String.format("%,d", curBal) + "  \u2022  Лимит: от 0 до 1 000 000",
                mx + mw / 2, my + 68 * scale, Math.round(15 * scale), false, Gfx.CENTER, th.dim, mw - 24);

        // Amount Box
        float abH = Math.round(48 * scale);
        g.color(0xFF0D0D12);
        g.rrect(mx + 20, my + 88 * scale, mw - 40, abH, 2.0f);
        g.color(ui.neon(0.1f));
        g.rrectStroke(mx + 20, my + 88 * scale, mw - 40, abH, 2.0f, 2.0f);
        String addStr = "+ " + String.format("%,d", app.addMoneyAmount) + " монет";
        ui.label(g, addStr, mx + mw / 2, my + 112 * scale, Math.round(24 * scale), true, Gfx.CENTER, 0xFFFFFFFF, mw - 60);

        // Slider track
        float slX = mx + 24, slY = my + 154 * scale, slW = mw - 48, slH = Math.round(16 * scale);
        g.color(0xFF1E293B);
        g.rrect(slX, slY, slW, slH, 2.0f);

        float pct = C.clamp(app.addMoneyAmount / 1000000f, 0f, 1f);
        g.color(th.accent(C.YELLOW));
        g.rrect(slX, slY, Math.max(8, slW * pct), slH, 2.0f);

        float thumbX = slX + slW * pct;
        g.color(0xFFFFFFFF);
        g.rrect(thumbX - 8, slY - 6, 16, slH + 12, 3.0f);
        g.color(0xFF000000);
        g.rrectStroke(thumbX - 8, slY - 6, 16, slH + 12, 3.0f, 1.5f);

        if (ui.down && Ui.in(ui.px, ui.py, slX - 20, slY - 20, slW + 40, slH + 40)) {
            float newPct = C.clamp((ui.px - slX) / slW, 0f, 1f);
            app.addMoneyAmount = Math.round(newPct * 1000f) * 1000;
        }

        ui.label(g, "0", slX, slY + 24 * scale, Math.round(12 * scale), false, Gfx.LEFT, th.dim, 50);
        ui.label(g, "500 000", slX + slW / 2, slY + 24 * scale, Math.round(12 * scale), false, Gfx.CENTER, th.dim, 80);
        ui.label(g, "1 000 000", slX + slW, slY + 24 * scale, Math.round(12 * scale), false, Gfx.RIGHT, th.dim, 80);

        // Large touch-friendly Preset Buttons - Row 1 (4 buttons)
        float yChips1 = my + 196 * scale;
        float cw1 = (mw - 48 - 3 * 10) / 4f;
        float chipH = Math.round(48 * scale);
        int[] row1Vals = {1000, 10000, 50000, 100000};
        String[] row1Labels = {"+1,000", "+10,000", "+50,000", "+100,000"};
        for (int i = 0; i < 4; i++) {
            if (ui.chip(g, row1Labels[i], slX + i * (cw1 + 10), yChips1, cw1, chipH, false, C.YELLOW)) {
                app.addMoneyAmount = Math.min(1000000, app.addMoneyAmount + row1Vals[i]);
            }
        }

        // Large touch-friendly Preset Buttons - Row 2 (3 buttons)
        float yChips2 = yChips1 + chipH + Math.round(10 * scale);
        float cw2 = (mw - 48 - 2 * 10) / 3f;
        if (ui.chip(g, "+500,000", slX, yChips2, cw2, chipH, false, C.YELLOW)) {
            app.addMoneyAmount = Math.min(1000000, app.addMoneyAmount + 500000);
        }
        if (ui.chip(g, "+1,000,000", slX + cw2 + 10, yChips2, cw2, chipH, false, C.YELLOW)) {
            app.addMoneyAmount = 1000000;
        }
        if (ui.chip(g, "MAX (1M)", slX + 2 * (cw2 + 10), yChips2, cw2, chipH, app.addMoneyAmount == 1000000, C.YELLOW)) {
            app.addMoneyAmount = 1000000;
        }

        // Action Buttons: Add & Set
        float btnY = yChips2 + chipH + Math.round(14 * scale);
        float btnW = (mw - 48 - 12) / 2;
        float btnH = Math.round(52 * scale);
        if (drawGreenNeonRectButton(g, ui, "Добавить", slX, btnY, btnW, btnH)) {
            int newB = Math.min(1000000, curBal + app.addMoneyAmount);
            app.store.putInt("slots.bal", newB);
            app.showMoneyModal = false;
        }
        if (drawNeonRectButton(g, ui, "Установить", slX + btnW + 12, btnY, btnW, btnH, C.CYAN)) {
            app.store.putInt("slots.bal", app.addMoneyAmount);
            app.showMoneyModal = false;
        }

        // Close Button
        float closeY = btnY + btnH + Math.round(10 * scale);
        if (drawNeonRectButton(g, ui, "Закрыть", slX, closeY, mw - 48, Math.round(44 * scale), C.PURPLE)) {
            app.showMoneyModal = false;
        }
    }

    // ------------------------------------------------------------------ Helpers
    private static boolean drawNeonRectButton(Gfx g, Ui ui, String label, float x, float y, float w, float h, int glowColor) {
        boolean hov = ui.hover(x, y, w, h) && !ui.down;
        boolean prs = ui.pressedIn(x, y, w, h) && ui.hover(x, y, w, h);
        float scale = Math.max(1f, h / 85f);
        float r = Math.max(3f, 4f * scale);

        // Multi-layered DropShadow glow matching sharedGlowEffect in PC version
        ui.dropShadowGlow(g, x, y, w, h, r, glowColor, hov || prs ? 1.8f : 1.3f);

        // Solid pure black fill
        g.color(prs ? 0xFF111827 : 0xFF000000);
        g.rrect(x, y, w, h, r);

        // Border matching PC version: white on hover/press, synchronized neon glow when idle
        g.color(hov || prs ? 0xFFFFFFFF : C.a(glowColor, 0.75f));
        g.rrectStroke(x, y, w, h, r, Math.max(2.2f, 2.8f * scale));

        // White bold text
        g.color(0xFFFFFFFF);
        float fsz = Math.min(36, h * 0.36f);
        g.text(label, x + w / 2, y + h / 2, fsz, true, Gfx.CENTER);

        return ui.clicked(x, y, w, h);
    }

    private static boolean drawGreenNeonRectButton(Gfx g, Ui ui, String label, float x, float y, float w, float h) {
        boolean hov = ui.hover(x, y, w, h) && !ui.down;
        boolean prs = ui.pressedIn(x, y, w, h) && ui.hover(x, y, w, h);
        float scale = Math.max(1f, h / 85f);
        float r = Math.max(3f, 4f * scale);

        // Green neon DropShadow glow
        ui.dropShadowGlow(g, x, y, w, h, r, 0xFF22C55E, hov || prs ? 1.8f : 1.2f);

        g.color(prs ? 0xFF052E16 : 0xFF000000);
        g.rrect(x, y, w, h, r);

        g.color(hov || prs ? 0xFFFFFFFF : 0xFF22C55E);
        g.rrectStroke(x, y, w, h, r, Math.max(2.5f, 3.0f * scale));

        g.color(0xFFFFFFFF);
        float fsz = Math.min(36, h * 0.36f);
        g.text(label, x + w / 2, y + h / 2, fsz, true, Gfx.CENTER);

        return ui.clicked(x, y, w, h);
    }

    private static int rainbowColor(float t) {
        int[] cols = {
            0xFF00FFFF, // Cyan
            0xFFA855F7, // Purple
            0xFFEF4444, // Red
            0xFFEAB308, // Yellow
            0xFF22C55E, // Green
            0xFF84CC16  // Lime
        };
        int idx = ((int) t) % cols.length;
        if (idx < 0) idx += cols.length;
        int next = (idx + 1) % cols.length;
        float f = t - (int) t;
        return C.mix(cols[idx], cols[next], f);
    }
}
