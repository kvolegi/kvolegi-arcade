package arcade.desktop;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Random;

public class MinesView {
    private int gridSize = 10;
    private int numMines = 15;
    private String difficulty = "Medium";

    private final Stage stage;
    private final Runnable onBackToArcade;

    private Button[][] buttons;
    private boolean[][] mines;
    private boolean[][] revealed;
    private boolean[][] flagged;

    private boolean flagMode = false;
    private boolean gameOver = false;
    private int flagsPlaced = 0;
    private int sonarCharges = 2;
    private boolean sonarActive = false;
    private Button sonarBtn;

    private Image beaverImg;
    private Image broccoliImg;

    private Button toggleModeBtn;
    private Label mineCountLabel;
    private Label statusLabel;
    private VBox overlayBox;
    private StackPane gridLayer;

    public MinesView(Stage stage, Runnable onBackToArcade) {
        this.stage = stage;
        this.onBackToArcade = onBackToArcade;
        loadImages();
    }

    public static void show(Stage stage, Runnable onBackToArcade) {
        MinesView view = new MinesView(stage, onBackToArcade);
        view.showPreGameMenu();
    }

    private void loadImages() {
        try {
            beaverImg = new Image(getClass().getResourceAsStream("/bobr_full.png"));
            if (beaverImg.isError() || beaverImg.getWidth() == 0) {
                beaverImg = new Image(getClass().getResourceAsStream("/bobr.png"));
            }
        } catch (Exception ignored) { }
        try {
            broccoliImg = new Image(getClass().getResourceAsStream("/broccoli.png"));
        } catch (Exception ignored) { }
    }

    public void showPreGameMenu() {
        BorderPane container = new BorderPane();
        container.setStyle("-fx-background-color: black;");

        VBox root = new VBox(22);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Label title = new Label("Minesweeper");
        title.setFont(GlowHelper.getFont(72, true));
        title.setTextFill(Color.WHITE);
        title.setEffect(GlowHelper.sharedGlowEffect);

        Label sub = new Label("Find the hidden beavers before they eat the broccoli!");
        sub.setFont(GlowHelper.getFont(17, false));
        sub.setTextFill(Color.web("#94a3b8"));
        sub.setMouseTransparent(true);

        // Difficulty selector
        Label diffLbl = new Label("Choose Difficulty / Level:");
        diffLbl.setFont(GlowHelper.getFont(18, true));
        diffLbl.setTextFill(Color.WHITE);
        diffLbl.setMouseTransparent(true);

        HBox diffBox = new HBox(14);
        diffBox.setAlignment(Pos.CENTER);
        diffBox.setPickOnBounds(false);

        Button easyBtn = GlowHelper.createNeonButton("Easy (8x8, 10)", 170, 50);
        Button medBtn = GlowHelper.createNeonButton("Medium (10x10, 15)", 210, 50);
        Button hardBtn = GlowHelper.createNeonButton("Hard (12x12, 25)", 180, 50);

        String activeStyle = "-fx-background-color: black; -fx-border-color: white; -fx-border-width: 3px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-cursor: hand; -fx-padding: 0;";
        String inactiveStyle = "-fx-background-color: black; -fx-border-color: #334155; -fx-border-width: 2px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-cursor: hand; -fx-padding: 0;";

        Runnable updateButtonStyles = () -> {
            easyBtn.setStyle("Easy".equals(difficulty) ? activeStyle : inactiveStyle);
            medBtn.setStyle("Medium".equals(difficulty) ? activeStyle : inactiveStyle);
            hardBtn.setStyle("Hard".equals(difficulty) ? activeStyle : inactiveStyle);
        };
        updateButtonStyles.run();

        easyBtn.setOnAction(e -> {
            difficulty = "Easy"; gridSize = 8; numMines = 10;
            updateButtonStyles.run();
        });
        medBtn.setOnAction(e -> {
            difficulty = "Medium"; gridSize = 10; numMines = 15;
            updateButtonStyles.run();
        });
        hardBtn.setOnAction(e -> {
            difficulty = "Hard"; gridSize = 12; numMines = 25;
            updateButtonStyles.run();
        });

        diffBox.getChildren().addAll(easyBtn, medBtn, hardBtn);

        Button startBtn = GlowHelper.createNeonButton("Start Game", 420, 80);
        startBtn.setOnAction(e -> showGame());

        Button backBtn = GlowHelper.createNeonButton("Arcade Hub", 420, 80);
        backBtn.setOnAction(e -> onBackToArcade.run());

        root.getChildren().addAll(title, sub, diffLbl, diffBox, startBtn, backBtn);
        container.setCenter(root);

        BorderPane footer = new BorderPane();
        footer.setPadding(new Insets(10, 24, 16, 24));
        footer.setLeft(GlowHelper.createWatermark());
        footer.setPickOnBounds(false);
        footer.setMouseTransparent(true);
        container.setBottom(footer);

        Scene scene = new Scene(container, 1050, 750);
        stage.setScene(scene);
    }

    public void showGame() {
        buttons = new Button[gridSize][gridSize];
        mines = new boolean[gridSize][gridSize];
        revealed = new boolean[gridSize][gridSize];
        flagged = new boolean[gridSize][gridSize];

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: black;");

        // Top bar
        BorderPane topBar = new BorderPane();
        topBar.setPadding(new Insets(12, 20, 8, 20));

        Label title = new Label("Minesweeper");
        title.setFont(GlowHelper.getFont(32, true));
        title.setTextFill(Color.WHITE);
        title.setEffect(GlowHelper.sharedGlowEffect);
        topBar.setLeft(title);

        mineCountLabel = new Label("Mines: " + numMines + " | Flags: 0");
        mineCountLabel.setFont(GlowHelper.getFont(17, true));
        mineCountLabel.setTextFill(Color.web("#eab308"));
        mineCountLabel.setEffect(GlowHelper.sharedGlowEffect);
        BorderPane.setAlignment(mineCountLabel, Pos.CENTER);
        topBar.setCenter(mineCountLabel);

        HBox topButtons = new HBox(10);
        topButtons.setAlignment(Pos.CENTER_RIGHT);

        sonarBtn = GlowHelper.createNeonButton("📡 Sonar (" + sonarCharges + ")", 145, 42);
        sonarBtn.setFont(GlowHelper.getFont(14, true));
        sonarBtn.setFocusTraversable(false);
        sonarBtn.setOnAction(e -> {
            if (sonarCharges <= 0 || gameOver) return;
            sonarActive = !sonarActive;
            updateSonarBtn();
        });

        Button resetBtn = GlowHelper.createNeonButton("Restart", 110, 42);
        resetBtn.setFont(GlowHelper.getFont(14, true));
        resetBtn.setFocusTraversable(false);
        resetBtn.setOnAction(e -> restartGame());

        Button settingsBtn = GlowHelper.createNeonButton("Settings", 110, 42);
        settingsBtn.setFont(GlowHelper.getFont(14, true));
        settingsBtn.setFocusTraversable(false);
        settingsBtn.setOnAction(e -> openSettingsModal());

        Button backBtn = GlowHelper.createNeonButton("Arcade Hub", 130, 42);
        backBtn.setFont(GlowHelper.getFont(14, true));
        backBtn.setFocusTraversable(false);
        backBtn.setOnAction(e -> onBackToArcade.run());

        topButtons.getChildren().addAll(sonarBtn, resetBtn, settingsBtn, backBtn);
        topBar.setRight(topButtons);
        root.setTop(topBar);

        // Center Grid: dynamically sized to fit screen without blue tint (pure solid black!)
        VBox centerBox = new VBox(10);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(6, 16, 10, 16));

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(3);
        grid.setVgap(3);
        grid.setStyle("-fx-border-color: #222222; -fx-border-width: 2px; -fx-border-radius: 4px; -fx-padding: 8; -fx-background-color: black;");

        final double cellSize = gridSize == 8 ? 52 : gridSize == 10 ? 46 : 39;
        String cellNormalStyle = "-fx-background-color: black; -fx-border-color: #262626; -fx-border-width: 1.5px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-cursor: hand; -fx-padding: 0;";
        String cellHoverStyle = "-fx-background-color: #1a1a1a; -fx-border-color: #555555; -fx-border-width: 1.5px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-cursor: hand; -fx-padding: 0;";

        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                Button btn = new Button(" ");
                btn.setFont(GlowHelper.getFont(Math.max(14, cellSize * 0.42), true));
                btn.setTextFill(Color.WHITE);
                btn.setStyle(cellNormalStyle);
                btn.setPadding(Insets.EMPTY);
                btn.setEllipsisString("");
                btn.setMinSize(cellSize, cellSize);
                btn.setPrefSize(cellSize, cellSize);
                btn.setMaxSize(cellSize, cellSize);
                btn.setFocusTraversable(false);

                final int row = r, col = c;
                btn.setOnMouseEntered(e -> {
                    if (!revealed[row][col] && !flagged[row][col]) {
                        btn.setStyle(cellHoverStyle);
                    }
                });
                btn.setOnMouseExited(e -> {
                    if (!revealed[row][col] && !flagged[row][col]) {
                        btn.setStyle(cellNormalStyle);
                    }
                });

                btn.setOnMouseClicked(e -> {
                    if (e.getButton() == MouseButton.SECONDARY) {
                        toggleFlag(row, col);
                    } else {
                        handleCellClick(row, col);
                    }
                });

                buttons[r][c] = btn;
                grid.add(btn, c, r);
            }
        }

        overlayBox = new VBox(14);
        overlayBox.setAlignment(Pos.CENTER);
        overlayBox.setStyle("-fx-background-color: rgba(0,0,0,0.85); -fx-padding: 20;");
        overlayBox.setVisible(false);

        gridLayer = new StackPane(grid, overlayBox);
        gridLayer.setMaxSize(gridSize * cellSize + 20, gridSize * cellSize + 20);

        // Bottom Controls
        HBox botControls = new HBox(16);
        botControls.setAlignment(Pos.CENTER);

        toggleModeBtn = GlowHelper.createNeonButton("Mode: Reveal", 160, 42);
        toggleModeBtn.setFont(GlowHelper.getFont(14, true));
        toggleModeBtn.setOnAction(e -> {
            flagMode = !flagMode;
            toggleModeBtn.setText(flagMode ? "Mode: Flag" : "Mode: Reveal");
            statusLabel.setText(flagMode ? "Clicking a cell places a broccoli flag" : "Clicking a cell reveals it");
        });

        statusLabel = new Label("Left-click to reveal, right-click to place a broccoli flag.");
        statusLabel.setFont(GlowHelper.getFont(14, false));
        statusLabel.setTextFill(Color.web("#94a3b8"));
        statusLabel.setMouseTransparent(true);

        botControls.getChildren().addAll(toggleModeBtn, statusLabel);
        centerBox.getChildren().addAll(gridLayer, botControls);
        root.setCenter(centerBox);

        // Footer: strictly watermark
        BorderPane footer = new BorderPane();
        footer.setPadding(new Insets(6, 20, 12, 20));
        footer.setLeft(GlowHelper.createWatermark());
        footer.setPickOnBounds(false);
        footer.setMouseTransparent(true);
        root.setBottom(footer);

        Scene scene = new Scene(root, 1050, 750);
        stage.setScene(scene);

        restartGame();
    }

    private void updateSonarBtn() {
        if (sonarBtn == null) return;
        sonarBtn.setText(sonarActive ? "🎯 SCANNING..." : "📡 Sonar (" + sonarCharges + ")");
        if (sonarActive) {
            sonarBtn.setStyle("-fx-background-color: #0c4a6e; -fx-border-color: #38bdf8; -fx-border-width: 2.5px; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 0;");
        } else {
            sonarBtn.setStyle("-fx-background-color: black; -fx-border-color: black; -fx-border-width: 2px; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 0;");
        }
    }

    private void restartGame() {
        gameOver = false;
        flagsPlaced = 0;
        flagMode = false;
        sonarCharges = 2;
        sonarActive = false;
        updateSonarBtn();
        if (toggleModeBtn != null) toggleModeBtn.setText("Mode: Reveal");
        if (overlayBox != null) overlayBox.setVisible(false);
        updateStats();

        String cellNormalStyle = "-fx-background-color: black; -fx-border-color: #262626; -fx-border-width: 1.5px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-cursor: hand; -fx-padding: 0;";
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                mines[r][c] = false;
                revealed[r][c] = false;
                flagged[r][c] = false;
                buttons[r][c].setText(" ");
                buttons[r][c].setGraphic(null);
                buttons[r][c].setDisable(false);
                buttons[r][c].setStyle(cellNormalStyle);
            }
        }

        // Place numMines randomly
        Random rnd = new Random();
        int placed = 0;
        while (placed < numMines) {
            int r = rnd.nextInt(gridSize);
            int c = rnd.nextInt(gridSize);
            if (!mines[r][c]) {
                mines[r][c] = true;
                placed++;
            }
        }
    }

    private void useSonarScan(int cr, int cc) {
        int detectedMines = 0;
        int safeRevealed = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = cr + dr, nc = cc + dc;
                if (nr >= 0 && nr < gridSize && nc >= 0 && nc < gridSize) {
                    if (mines[nr][nc]) {
                        detectedMines++;
                        if (!flagged[nr][nc] && !revealed[nr][nc]) {
                            flagged[nr][nc] = true;
                            flagsPlaced++;
                            buttons[nr][nc].setText("⚓");
                            buttons[nr][nc].setTextFill(Color.web("#38bdf8"));
                            buttons[nr][nc].setStyle("-fx-background-color: #0c4a6e; -fx-border-color: #38bdf8; -fx-border-width: 2px; -fx-padding: 0;");
                        }
                    } else if (!revealed[nr][nc] && !flagged[nr][nc]) {
                        revealCell(nr, nc);
                        safeRevealed++;
                    }
                }
            }
        }
        updateStats();
        if (checkWin()) {
            gameOver = true;
            statusLabel.setText("Victory! All safe cells cleared!");
            statusLabel.setTextFill(Color.web("#22c55e"));
            showEndOverlay("VICTORY!", "Congratulations! All " + numMines + " mines cleared!", "#22c55e");
        } else {
            statusLabel.setText("📡 SONAR: Sector scanned! " + detectedMines + " mines located, " + safeRevealed + " safe cells revealed.");
            statusLabel.setTextFill(Color.web("#38bdf8"));
        }
    }

    private void openSettingsModal() {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(stage);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: black; -fx-padding: 30;");

        Label title = new Label("Minesweeper Settings");
        title.setFont(GlowHelper.getFont(26, true));
        title.setTextFill(Color.WHITE);
        title.setEffect(GlowHelper.sharedGlowEffect);

        // Difficulty selection
        Label diffLbl = new Label("Field Size / Difficulty:");
        diffLbl.setFont(GlowHelper.getFont(16, true));
        diffLbl.setTextFill(Color.WHITE);

        HBox diffBox = new HBox(10);
        diffBox.setAlignment(Pos.CENTER);

        final String[] chosenDiff = {difficulty};
        Button easyBtn = GlowHelper.createNeonButton("Easy (8x8, 10)", 150, 44);
        Button medBtn = GlowHelper.createNeonButton("Medium (10x10, 15)", 175, 44);
        Button hardBtn = GlowHelper.createNeonButton("Hard (12x12, 25)", 160, 44);

        String activeStyle = "-fx-background-color: black; -fx-border-color: white; -fx-border-width: 2.5px; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-cursor: hand; -fx-padding: 0;";
        String inactiveStyle = "-fx-background-color: black; -fx-border-color: #334155; -fx-border-width: 1.5px; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-cursor: hand; -fx-padding: 0;";

        Runnable updateStyle = () -> {
            easyBtn.setStyle("Easy".equals(chosenDiff[0]) ? activeStyle : inactiveStyle);
            medBtn.setStyle("Medium".equals(chosenDiff[0]) ? activeStyle : inactiveStyle);
            hardBtn.setStyle("Hard".equals(chosenDiff[0]) ? activeStyle : inactiveStyle);
        };
        updateStyle.run();

        easyBtn.setOnAction(e -> { chosenDiff[0] = "Easy"; updateStyle.run(); });
        medBtn.setOnAction(e -> { chosenDiff[0] = "Medium"; updateStyle.run(); });
        hardBtn.setOnAction(e -> { chosenDiff[0] = "Hard"; updateStyle.run(); });

        diffBox.getChildren().addAll(easyBtn, medBtn, hardBtn);

        // Sonar Charges
        HBox sonarBox = new HBox(12);
        sonarBox.setAlignment(Pos.CENTER);
        Label sonarLbl = new Label("Sonar Radar Scans:");
        sonarLbl.setFont(GlowHelper.getFont(16, true));
        sonarLbl.setTextFill(Color.WHITE);

        final int[] chosenSonar = {sonarCharges};
        Button s1 = GlowHelper.createNeonButton("1", 48, 40);
        Button s2 = GlowHelper.createNeonButton("2", 48, 40);
        Button s3 = GlowHelper.createNeonButton("3", 48, 40);
        Runnable updateSonarStyle = () -> {
            s1.setStyle(chosenSonar[0] == 1 ? activeStyle : inactiveStyle);
            s2.setStyle(chosenSonar[0] == 2 ? activeStyle : inactiveStyle);
            s3.setStyle(chosenSonar[0] == 3 ? activeStyle : inactiveStyle);
        };
        updateSonarStyle.run();
        s1.setOnAction(e -> { chosenSonar[0] = 1; updateSonarStyle.run(); });
        s2.setOnAction(e -> { chosenSonar[0] = 2; updateSonarStyle.run(); });
        s3.setOnAction(e -> { chosenSonar[0] = 3; updateSonarStyle.run(); });
        sonarBox.getChildren().addAll(sonarLbl, s1, s2, s3);

        Button applyBtn = GlowHelper.createNeonButton("Apply & Restart Game", 240, 52);
        applyBtn.setOnAction(e -> {
            difficulty = chosenDiff[0];
            if ("Easy".equals(difficulty)) { gridSize = 8; numMines = 10; }
            else if ("Medium".equals(difficulty)) { gridSize = 10; numMines = 15; }
            else { gridSize = 12; numMines = 25; }
            sonarCharges = chosenSonar[0];
            modal.close();
            showGame();
        });

        root.getChildren().addAll(title, diffLbl, diffBox, sonarBox, applyBtn, GlowHelper.createWatermark());
        Scene sc = new Scene(root, 560, 430);
        modal.setScene(sc);
        modal.setTitle("Minesweeper Settings");
        modal.show();
    }

    private void handleCellClick(int r, int c) {
        if (gameOver) return;

        if (sonarActive) {
            sonarActive = false;
            if (sonarCharges > 0) sonarCharges--;
            updateSonarBtn();
            useSonarScan(r, c);
            return;
        }

        if (flagMode) {
            toggleFlag(r, c);
            return;
        }

        if (flagged[r][c] || revealed[r][c]) return;

        if (mines[r][c]) {
            revealAllMines();
            gameOver = true;
            statusLabel.setText("Mine detonated! Game Over.");
            statusLabel.setTextFill(Color.web("#ef4444"));

            showEndOverlay("GAME OVER", "A mine detonated! Be careful next time.", "#ef4444");
        } else {
            revealCell(r, c);
            if (checkWin()) {
                gameOver = true;
                statusLabel.setText("Victory! All safe cells cleared!");
                statusLabel.setTextFill(Color.web("#22c55e"));

                showEndOverlay("VICTORY!", "Congratulations! All " + numMines + " mines safely cleared!", "#22c55e");
            }
        }
    }

    private void showEndOverlay(String titleText, String subText, String accentColor) {
        overlayBox.getChildren().clear();

        Label tLbl = new Label(titleText);
        tLbl.setFont(GlowHelper.getFont(32, true));
        tLbl.setTextFill(Color.web(accentColor));
        tLbl.setEffect(GlowHelper.sharedGlowEffect);

        Label sLbl = new Label(subText);
        sLbl.setFont(GlowHelper.getFont(16, true));
        sLbl.setTextFill(Color.WHITE);

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER);

        Button playAgain = GlowHelper.createNeonButton("Play Again", 140, 45);
        playAgain.setOnAction(e -> restartGame());

        Button changeDiff = GlowHelper.createNeonButton("Change Level", 150, 45);
        changeDiff.setOnAction(e -> showPreGameMenu());

        Button hubBtn = GlowHelper.createNeonButton("Arcade Hub", 140, 45);
        hubBtn.setOnAction(e -> onBackToArcade.run());

        btns.getChildren().addAll(playAgain, changeDiff, hubBtn);
        overlayBox.getChildren().addAll(tLbl, sLbl, btns);
        overlayBox.setVisible(true);
    }

    private void toggleFlag(int r, int c) {
        if (gameOver || revealed[r][c]) return;

        if (flagged[r][c]) {
            flagged[r][c] = false;
            buttons[r][c].setGraphic(null);
            buttons[r][c].setText(" ");
            flagsPlaced--;
        } else {
            if (flagsPlaced < numMines) {
                flagged[r][c] = true;
                if (broccoliImg != null && !broccoliImg.isError()) {
                    ImageView iv = new ImageView(broccoliImg);
                    double iconSz = gridSize == 8 ? 32 : gridSize == 10 ? 28 : 24;
                    iv.setFitWidth(iconSz);
                    iv.setFitHeight(iconSz);
                    iv.setPreserveRatio(true);
                    buttons[r][c].setGraphic(iv);
                    buttons[r][c].setText("");
                } else {
                    buttons[r][c].setText("🥦");
                }
                flagsPlaced++;
            } else {
                statusLabel.setText("Cannot place more flags than mines!");
            }
        }
        updateStats();
    }

    private void revealCell(int r, int c) {
        if (r < 0 || r >= gridSize || c < 0 || c >= gridSize || revealed[r][c] || flagged[r][c]) {
            return;
        }

        revealed[r][c] = true;
        int count = countAdjacentMines(r, c);
        Button btn = buttons[r][c];
        btn.setStyle("-fx-background-color: #0d0d0d; -fx-border-color: #1c1c1c; -fx-border-width: 1px; -fx-border-radius: 2px; -fx-padding: 0;");

        if (count > 0) {
            btn.setText(String.valueOf(count));
            String col = count == 1 ? "#38bdf8" : count == 2 ? "#4ade80" : count == 3 ? "#facc15" : count == 4 ? "#c084fc" : "#f87171";
            btn.setTextFill(Color.web(col));
        } else {
            btn.setText("");
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    revealCell(r + dr, c + dc);
                }
            }
        }
    }

    private int countAdjacentMines(int r, int c) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = r + dr, nc = c + dc;
                if (nr >= 0 && nr < gridSize && nc >= 0 && nc < gridSize && mines[nr][nc]) {
                    count++;
                }
            }
        }
        return count;
    }

    private void revealAllMines() {
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                if (mines[r][c]) {
                    if (beaverImg != null && !beaverImg.isError()) {
                        ImageView iv = new ImageView(beaverImg);
                        double iconSz = gridSize == 8 ? 36 : gridSize == 10 ? 32 : 26;
                        iv.setFitWidth(iconSz);
                        iv.setFitHeight(iconSz);
                        iv.setPreserveRatio(true);
                        buttons[r][c].setGraphic(iv);
                        buttons[r][c].setText("");
                    } else {
                        buttons[r][c].setText("🦫");
                    }
                    buttons[r][c].setStyle("-fx-background-color: #260000; -fx-border-color: #ef4444; -fx-border-width: 2px; -fx-padding: 0;");
                }
            }
        }
    }

    private boolean checkWin() {
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                if (!mines[r][c] && !revealed[r][c]) return false;
            }
        }
        return true;
    }

    private void updateStats() {
        if (mineCountLabel != null) {
            mineCountLabel.setText("Mines: " + numMines + " | Flags: " + flagsPlaced);
        }
    }
}
