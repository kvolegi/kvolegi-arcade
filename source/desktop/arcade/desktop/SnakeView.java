package arcade.desktop;

import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeView {
    // Compact field size cut in half per user request! Much better visibility!
    private int cols = 15;
    private int rows = 12;
    private final int tileSize = 38;

    private final Stage stage;
    private final Runnable onBackToArcade;

    private final ArrayList<Point> snake = new ArrayList<>();
    private Point beerPoint;
    private final List<Point> broccoliPoints = new ArrayList<>();
    private final List<Point> obstacles = new ArrayList<>();

    private char direction = 'R';
    private char nextDirection = 'R';
    private boolean inGame = false;
    private boolean isPaused = false;
    private boolean isVictory = false;

    private int score = 0;
    private int level = 1;
    private int beersInLevel = 0;
    private final int BEERS_PER_LEVEL = 4;
    private boolean isEndlessMode = false;
    private int baseSpeedMs = 220; // Calm, relaxed starting speed
    private boolean wallCollision = true; // True = Snake dies on hitting wall, False = wrap around

    private Color snakeColor = Color.web("#22c55e");
    private Image beerImg;
    private Image broccoliImg;

    private AnimationTimer gameLoop;
    private long lastTick = 0;
    private Canvas canvas;
    private Label scoreLabel;
    private Label statusLabel;
    private BorderPane canvasContainer;
    private StackPane gameLayer;
    private VBox overlayBox;

    public SnakeView(Stage stage, Runnable onBackToArcade) {
        this.stage = stage;
        this.onBackToArcade = onBackToArcade;
        loadImages();
    }

    public static void show(Stage stage, Runnable onBackToArcade) {
        SnakeView view = new SnakeView(stage, onBackToArcade);
        view.showPreGameMenu();
    }

    public void showPreGameMenu() {
        BorderPane container = new BorderPane();
        container.setStyle("-fx-background-color: black;");

        VBox root = new VBox(22);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Label title = new Label("Beer Snake");
        title.setFont(GlowHelper.getFont(72, true));
        title.setTextFill(Color.WHITE);
        title.setEffect(GlowHelper.sharedGlowEffect);

        // Mode selection
        HBox modeBox = new HBox(16);
        modeBox.setAlignment(Pos.CENTER);
        modeBox.setPickOnBounds(false);
        Label modeLbl = new Label("Mode:");
        modeLbl.setFont(GlowHelper.getFont(18, true));
        modeLbl.setTextFill(Color.WHITE);
        modeLbl.setMouseTransparent(true);
        Button modeBtn = GlowHelper.createNeonButton(isEndlessMode ? "Endless" : "25 Levels", 180, 48);
        modeBtn.setOnAction(e -> {
            isEndlessMode = !isEndlessMode;
            modeBtn.setText(isEndlessMode ? "Endless" : "25 Levels");
        });
        modeBox.getChildren().addAll(modeLbl, modeBtn);

        // Speed selection
        HBox speedBox = new HBox(16);
        speedBox.setAlignment(Pos.CENTER);
        speedBox.setPickOnBounds(false);
        Label speedLbl = new Label("Speed:");
        speedLbl.setFont(GlowHelper.getFont(18, true));
        speedLbl.setTextFill(Color.WHITE);
        speedLbl.setMouseTransparent(true);
        String speedName = baseSpeedMs >= 200 ? "Calm" : baseSpeedMs >= 150 ? "Normal" : "Fast";
        Button speedBtn = GlowHelper.createNeonButton(speedName, 180, 48);
        speedBtn.setOnAction(e -> {
            if (baseSpeedMs >= 200) {
                baseSpeedMs = 150;
                speedBtn.setText("Normal");
            } else if (baseSpeedMs >= 150) {
                baseSpeedMs = 100;
                speedBtn.setText("Fast");
            } else {
                baseSpeedMs = 220;
                speedBtn.setText("Calm");
            }
        });
        speedBox.getChildren().addAll(speedLbl, speedBtn);

        // Wall collision selection (Solid vs Wrap)
        HBox wallBox = new HBox(16);
        wallBox.setAlignment(Pos.CENTER);
        wallBox.setPickOnBounds(false);
        Label wallLbl = new Label("Walls:");
        wallLbl.setFont(GlowHelper.getFont(18, true));
        wallLbl.setTextFill(Color.WHITE);
        wallLbl.setMouseTransparent(true);
        Button wallBtn = GlowHelper.createNeonButton(wallCollision ? "Solid (Fatal)" : "Pass-Through", 180, 48);
        wallBtn.setOnAction(e -> {
            wallCollision = !wallCollision;
            wallBtn.setText(wallCollision ? "Solid (Fatal)" : "Pass-Through");
        });
        wallBox.getChildren().addAll(wallLbl, wallBtn);

        Button startBtn = GlowHelper.createNeonButton("Start Game", 420, 80);
        startBtn.setOnAction(e -> showScreen());

        Button backBtn = GlowHelper.createNeonButton("Arcade Hub", 420, 80);
        backBtn.setOnAction(e -> onBackToArcade.run());

        root.getChildren().addAll(title, modeBox, speedBox, wallBox, startBtn, backBtn);
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

    private void loadImages() {
        try {
            beerImg = new Image(getClass().getResourceAsStream("/beer.png"));
            if (beerImg.isError() || beerImg.getWidth() == 0) {
                beerImg = new Image(getClass().getResourceAsStream("/apple.png"));
            }
        } catch (Exception ignored) { }
        try {
            broccoliImg = new Image(getClass().getResourceAsStream("/broccoli.png"));
        } catch (Exception ignored) { }
    }

    public void showScreen() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: black;");

        // Top bar
        BorderPane topBar = new BorderPane();
        topBar.setPadding(new Insets(12, 20, 8, 20));

        Label title = new Label("Beer Snake");
        title.setFont(GlowHelper.getFont(32, true));
        title.setTextFill(Color.WHITE);
        title.setEffect(GlowHelper.sharedGlowEffect);
        topBar.setLeft(title);

        scoreLabel = new Label("Score: 0 | Level 1/25 (0/4) | Speed: Calm");
        scoreLabel.setFont(GlowHelper.getFont(17, true));
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setEffect(GlowHelper.sharedGlowEffect);
        BorderPane.setAlignment(scoreLabel, Pos.CENTER);
        topBar.setCenter(scoreLabel);

        HBox topButtons = new HBox(10);
        topButtons.setAlignment(Pos.CENTER_RIGHT);

        Button settingsBtn = GlowHelper.createNeonButton("Settings", 115, 42);
        settingsBtn.setFont(GlowHelper.getFont(14, true));
        settingsBtn.setFocusTraversable(false);
        settingsBtn.setOnAction(e -> openSettingsModal());

        Button restartBtn = GlowHelper.createNeonButton("Restart", 115, 42);
        restartBtn.setFont(GlowHelper.getFont(14, true));
        restartBtn.setFocusTraversable(false);
        restartBtn.setOnAction(e -> initGame());

        Button menuBtn = GlowHelper.createNeonButton("Menu", 115, 42);
        menuBtn.setFont(GlowHelper.getFont(14, true));
        menuBtn.setFocusTraversable(false);
        menuBtn.setOnAction(e -> {
            if (gameLoop != null) gameLoop.stop();
            showPreGameMenu();
        });

        Button backBtn = GlowHelper.createNeonButton("Arcade Hub", 130, 42);
        backBtn.setFont(GlowHelper.getFont(14, true));
        backBtn.setFocusTraversable(false);
        backBtn.setOnAction(e -> {
            if (gameLoop != null) gameLoop.stop();
            onBackToArcade.run();
        });

        topButtons.getChildren().addAll(settingsBtn, restartBtn, menuBtn, backBtn);
        topBar.setRight(topButtons);
        root.setTop(topBar);

        // Center: Canvas inside gameLayer
        VBox centerBox = new VBox(8);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(6, 16, 10, 16));

        canvas = new Canvas(cols * tileSize, rows * tileSize);
        canvasContainer = new BorderPane(canvas);
        canvasContainer.setMaxSize(cols * tileSize + 4, rows * tileSize + 4);
        canvasContainer.setStyle("-fx-border-color: #262626; -fx-border-width: 2px; -fx-border-radius: 4px; -fx-background-color: black;");
        canvasContainer.setEffect(GlowHelper.sharedGlowEffect);

        // In-game overlay for Game Over / Level Complete with clickable buttons
        overlayBox = new VBox(14);
        overlayBox.setAlignment(Pos.CENTER);
        overlayBox.setStyle("-fx-background-color: rgba(0,0,0,0.85); -fx-padding: 20;");
        overlayBox.setVisible(false);

        gameLayer = new StackPane(canvasContainer, overlayBox);
        gameLayer.setMaxSize(cols * tileSize + 6, rows * tileSize + 6);

        statusLabel = new Label("Controls: Arrow keys / WASD. Space to Pause. Eat beer to advance!");
        statusLabel.setFont(GlowHelper.getFont(14, false));
        statusLabel.setTextFill(Color.web("#94a3b8"));
        statusLabel.setMouseTransparent(true);

        centerBox.getChildren().addAll(gameLayer, statusLabel);
        root.setCenter(centerBox);

        // Footer: watermark strictly subtle @kvolegi
        BorderPane footer = new BorderPane();
        footer.setPadding(new Insets(6, 20, 12, 20));
        footer.setLeft(GlowHelper.createWatermark());
        footer.setPickOnBounds(false);
        footer.setMouseTransparent(true);
        root.setBottom(footer);

        Scene scene = new Scene(root, 1050, 750);

        // Global EventFilter captures arrow keys and WASD before buttons can consume them
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode code = e.getCode();
            boolean handled = true;
            if ((code == KeyCode.UP || code == KeyCode.W) && direction != 'D') nextDirection = 'U';
            else if ((code == KeyCode.DOWN || code == KeyCode.S) && direction != 'U') nextDirection = 'D';
            else if ((code == KeyCode.LEFT || code == KeyCode.A) && direction != 'R') nextDirection = 'L';
            else if ((code == KeyCode.RIGHT || code == KeyCode.D) && direction != 'L') nextDirection = 'R';
            else if (code == KeyCode.SPACE) {
                if (!inGame) {
                    initGame();
                } else {
                    isPaused = !isPaused;
                    statusLabel.setText(isPaused ? "PAUSED (Press SPACE to resume)" : "PLAYING");
                }
            } else if (code == KeyCode.ENTER && !inGame) {
                initGame();
            } else {
                handled = false;
            }
            if (handled) e.consume();
        });

        root.setFocusTraversable(true);
        stage.setScene(scene);
        Platform.runLater(root::requestFocus);
        initGame();
    }

    private void openSettingsModal() {
        if (gameLoop != null) isPaused = true;

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(stage);

        VBox root = new VBox(18);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: black; -fx-padding: 25;");

        Label title = new Label("Snake Settings");
        title.setFont(GlowHelper.getFont(26, true));
        title.setTextFill(Color.WHITE);
        title.setEffect(GlowHelper.sharedGlowEffect);

        // Mode: 25 Levels vs Endless
        HBox modeBox = new HBox(12);
        modeBox.setAlignment(Pos.CENTER);
        Label modeLbl = new Label("Game Mode:");
        modeLbl.setFont(GlowHelper.getFont(16, true));
        modeLbl.setTextFill(Color.WHITE);

        Button modeBtn = GlowHelper.createNeonButton(isEndlessMode ? "Endless" : "25 Levels", 160, 42);
        modeBtn.setFont(GlowHelper.getFont(14, true));
        modeBtn.setOnAction(e -> {
            isEndlessMode = !isEndlessMode;
            modeBtn.setText(isEndlessMode ? "Endless" : "25 Levels");
        });
        modeBox.getChildren().addAll(modeLbl, modeBtn);

        // Start Level slider (if Levels mode)
        HBox lvlBox = new HBox(12);
        lvlBox.setAlignment(Pos.CENTER);
        Label lvlLbl = new Label("Start Level: " + level);
        lvlLbl.setFont(GlowHelper.getFont(16, true));
        lvlLbl.setTextFill(Color.WHITE);

        Slider lvlSlider = new Slider(1, 25, level);
        lvlSlider.setMaxWidth(160);
        lvlSlider.valueProperty().addListener((obs, oldV, newV) -> {
            level = newV.intValue();
            lvlLbl.setText("Start Level: " + level);
        });
        lvlBox.getChildren().addAll(lvlLbl, lvlSlider);

        // Field Size Toggle: Compact (15x12) vs Standard (18x14)
        HBox sizeBox = new HBox(12);
        sizeBox.setAlignment(Pos.CENTER);
        Label sizeLbl = new Label("Field Size:");
        sizeLbl.setFont(GlowHelper.getFont(16, true));
        sizeLbl.setTextFill(Color.WHITE);

        Button sizeBtn = GlowHelper.createNeonButton(cols == 15 ? "Compact (15x12)" : "Wide (18x14)", 180, 42);
        sizeBtn.setFont(GlowHelper.getFont(14, true));
        sizeBtn.setOnAction(e -> {
            if (cols == 15) {
                cols = 18; rows = 14;
            } else {
                cols = 15; rows = 12;
            }
            sizeBtn.setText(cols == 15 ? "Compact (15x12)" : "Wide (18x14)");
            canvas.setWidth(cols * tileSize);
            canvas.setHeight(rows * tileSize);
            canvasContainer.setMaxSize(cols * tileSize + 4, rows * tileSize + 4);
            gameLayer.setMaxSize(cols * tileSize + 6, rows * tileSize + 6);
        });
        sizeBox.getChildren().addAll(sizeLbl, sizeBtn);

        // Speed: Calm (220ms) vs Normal (160ms) vs Fast (110ms)
        HBox speedBox = new HBox(12);
        speedBox.setAlignment(Pos.CENTER);
        Label speedLbl = new Label("Initial Speed:");
        speedLbl.setFont(GlowHelper.getFont(16, true));
        speedLbl.setTextFill(Color.WHITE);

        String speedName = baseSpeedMs >= 200 ? "Calm" : baseSpeedMs >= 150 ? "Normal" : "Fast";
        Button speedBtn = GlowHelper.createNeonButton(speedName, 140, 42);
        speedBtn.setFont(GlowHelper.getFont(14, true));
        speedBtn.setOnAction(e -> {
            if (baseSpeedMs >= 200) {
                baseSpeedMs = 150;
                speedBtn.setText("Normal");
            } else if (baseSpeedMs >= 150) {
                baseSpeedMs = 100;
                speedBtn.setText("Fast");
            } else {
                baseSpeedMs = 220;
                speedBtn.setText("Calm");
            }
        });
        speedBox.getChildren().addAll(speedLbl, speedBtn);

        // Wall collision selection (Solid vs Wrap)
        HBox wallBox = new HBox(12);
        wallBox.setAlignment(Pos.CENTER);
        Label wallLbl = new Label("Walls:");
        wallLbl.setFont(GlowHelper.getFont(16, true));
        wallLbl.setTextFill(Color.WHITE);
        Button wallBtn = GlowHelper.createNeonButton(wallCollision ? "Solid (Fatal)" : "Pass-Through", 160, 42);
        wallBtn.setFont(GlowHelper.getFont(14, true));
        wallBtn.setOnAction(e -> {
            wallCollision = !wallCollision;
            wallBtn.setText(wallCollision ? "Solid (Fatal)" : "Pass-Through");
        });
        wallBox.getChildren().addAll(wallLbl, wallBtn);

        Button applyBtn = GlowHelper.createNeonButton("Apply & Restart", 200, 48);
        applyBtn.setOnAction(e -> {
            modal.close();
            initGame();
        });

        root.getChildren().addAll(title, modeBox, lvlBox, sizeBox, speedBox, wallBox, applyBtn, GlowHelper.createWatermark());

        Scene sc = new Scene(root, 480, 510);
        modal.setScene(sc);
        modal.setTitle("Snake Settings");
        modal.show();
    }

    private void initGame() {
        beersInLevel = 0;
        score = 0;
        direction = 'R';
        nextDirection = 'R';
        inGame = true;
        isPaused = false;
        isVictory = false;
        snakeColor = Color.web("#22c55e");

        overlayBox.setVisible(false);

        snake.clear();
        int startY = rows / 2;
        snake.add(new Point(5, startY));
        snake.add(new Point(4, startY));
        snake.add(new Point(3, startY));

        setupLevelObstacles(level);
        spawnBeer();
        spawnBroccoli();

        updateScoreLabel();
        statusLabel.setText("Eat beer to grow! Reach " + BEERS_PER_LEVEL + " beers to advance to Level " + (level + 1));
        statusLabel.setTextFill(Color.web("#22c55e"));

        if (gameLoop != null) gameLoop.stop();
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Smooth variable tick delay based on current level & speed setting
                long delayMs = Math.max(75, baseSpeedMs - (level - 1) * 5);
                long delayNs = delayMs * 1_000_000L;
                if (now - lastTick >= delayNs) {
                    lastTick = now;
                    if (inGame && !isPaused) {
                        direction = nextDirection;
                        tick();
                    }
                    render();
                }
            }
        };
        gameLoop.start();
    }

    // 25 Canonical level obstacle patterns
    private void setupLevelObstacles(int lvl) {
        obstacles.clear();
        if (isEndlessMode) {
            // Endless: sparse random obstacles
            Random rnd = new Random();
            int count = Math.min(10, lvl * 2);
            for (int i = 0; i < count; i++) {
                int ox = rnd.nextInt(cols - 4) + 2;
                int oy = rnd.nextInt(rows - 4) + 2;
                obstacles.add(new Point(ox, oy));
            }
            return;
        }

        int cx = cols / 2;
        int cy = rows / 2;

        switch (lvl) {
            case 1:
                // Level 1: Open field (no obstacles)
                break;
            case 2:
                // Level 2: 2 central barrier blocks
                obstacles.add(new Point(cx, cy - 2));
                obstacles.add(new Point(cx, cy + 2));
                break;
            case 3:
                // Level 3: 4 corner barrier blocks
                obstacles.add(new Point(2, 2));
                obstacles.add(new Point(cols - 3, 2));
                obstacles.add(new Point(2, rows - 3));
                obstacles.add(new Point(cols - 3, rows - 3));
                break;
            case 4:
                // Level 4: Center '+' cross
                obstacles.add(new Point(cx, cy));
                obstacles.add(new Point(cx - 1, cy));
                obstacles.add(new Point(cx + 1, cy));
                obstacles.add(new Point(cx, cy - 1));
                obstacles.add(new Point(cx, cy + 1));
                break;
            case 5:
                // Level 5: Horizontal gates
                for (int x = 3; x < cols - 3; x++) {
                    if (x != cx) {
                        obstacles.add(new Point(x, 3));
                        obstacles.add(new Point(x, rows - 4));
                    }
                }
                break;
            case 6:
                // Level 6: 4 quadrant pillars
                obstacles.add(new Point(4, 3));
                obstacles.add(new Point(5, 3));
                obstacles.add(new Point(cols - 5, 3));
                obstacles.add(new Point(cols - 6, 3));
                obstacles.add(new Point(4, rows - 4));
                obstacles.add(new Point(5, rows - 4));
                obstacles.add(new Point(cols - 5, rows - 4));
                obstacles.add(new Point(cols - 6, rows - 4));
                break;
            case 7:
                // Level 7: Slalom bars
                for (int y = 2; y < rows - 2; y++) {
                    if (y != cy) obstacles.add(new Point(5, y));
                    if (y != cy + 1) obstacles.add(new Point(cols - 6, y));
                }
                break;
            case 8:
                // Level 8: The cyber ring with doorway
                for (int x = cx - 2; x <= cx + 2; x++) {
                    if (x != cx) obstacles.add(new Point(x, cy - 2));
                    if (x != cx) obstacles.add(new Point(x, cy + 2));
                }
                obstacles.add(new Point(cx - 2, cy - 1));
                obstacles.add(new Point(cx + 2, cy + 1));
                break;
            default:
                // Levels 9 to 25: procedural canonical layouts
                int seedCount = Math.min(14, 4 + lvl / 2);
                Random r = new Random(lvl * 31415L);
                for (int i = 0; i < seedCount; i++) {
                    int ox = r.nextInt(cols - 4) + 2;
                    int oy = r.nextInt(rows - 4) + 2;
                    if (Math.abs(ox - 5) > 1 || Math.abs(oy - cy) > 1) {
                        obstacles.add(new Point(ox, oy));
                    }
                }
                break;
        }
    }

    private void spawnBeer() {
        Random rnd = new Random();
        Point p;
        do {
            p = new Point(rnd.nextInt(cols), rnd.nextInt(rows));
        } while (isOccupied(p));
        beerPoint = p;
    }

    private void spawnBroccoli() {
        broccoliPoints.clear();
        int count = Math.min(3, 1 + level / 8);
        Random rnd = new Random();
        for (int i = 0; i < count; i++) {
            Point p;
            do {
                p = new Point(rnd.nextInt(cols), rnd.nextInt(rows));
            } while (isOccupied(p));
            broccoliPoints.add(p);
        }
    }

    private boolean isOccupied(Point p) {
        for (Point sp : snake) if (sp.equals(p)) return true;
        for (Point op : obstacles) if (op.equals(p)) return true;
        for (Point bp : broccoliPoints) if (bp.equals(p)) return true;
        return beerPoint != null && beerPoint.equals(p);
    }

    private void tick() {
        Point head = snake.get(0);
        Point newHead = new Point(head);

        switch (direction) {
            case 'U': newHead.y--; break;
            case 'D': newHead.y++; break;
            case 'L': newHead.x--; break;
            case 'R': newHead.x++; break;
        }

        // Wall collision or wrap-around
        if (wallCollision) {
            if (newHead.x < 0 || newHead.x >= cols || newHead.y < 0 || newHead.y >= rows) {
                gameOver();
                return;
            }
        } else {
            if (newHead.x < 0) newHead.x = cols - 1;
            else if (newHead.x >= cols) newHead.x = 0;
            if (newHead.y < 0) newHead.y = rows - 1;
            else if (newHead.y >= rows) newHead.y = 0;
        }

        // Collision with self
        for (Point sp : snake) {
            if (newHead.equals(sp)) {
                gameOver();
                return;
            }
        }

        // Collision with obstacles
        for (Point op : obstacles) {
            if (newHead.equals(op)) {
                gameOver();
                return;
            }
        }

        snake.add(0, newHead);

        // Check beer eaten
        if (beerPoint != null && newHead.equals(beerPoint)) {
            score += 10;
            beersInLevel++;

            if (!isEndlessMode && beersInLevel >= BEERS_PER_LEVEL) {
                if (level >= 25) {
                    victory();
                    return;
                } else {
                    advanceLevel();
                    return;
                }
            }

            updateScoreLabel();
            spawnBeer();
        } else {
            // Check broccoli collision
            boolean hitBroccoli = false;
            for (int i = 0; i < broccoliPoints.size(); i++) {
                if (newHead.equals(broccoliPoints.get(i))) {
                    hitBroccoli = true;
                    broccoliPoints.remove(i);
                    score = Math.max(0, score - 5);
                    if (snake.size() > 2) {
                        snake.remove(snake.size() - 1);
                    }
                    Random r = new Random();
                    snakeColor = Color.rgb(r.nextInt(180) + 70, r.nextInt(180) + 70, r.nextInt(180) + 70);
                    spawnBroccoli();
                    break;
                }
            }

            if (!hitBroccoli) {
                snake.remove(snake.size() - 1);
            }
            updateScoreLabel();
        }
    }

    private void advanceLevel() {
        level++;
        beersInLevel = 0;
        setupLevelObstacles(level);
        spawnBeer();
        spawnBroccoli();
        updateScoreLabel();

        statusLabel.setText("LEVEL " + level + " UNLOCKED! Speed increased.");
        statusLabel.setTextFill(Color.web("#facc15"));
    }

    private void gameOver() {
        inGame = false;
        if (gameLoop != null) gameLoop.stop();
        statusLabel.setText("GAME OVER! Final Score: " + score);
        statusLabel.setTextFill(Color.web("#ef4444"));

        showOverlay("GAME OVER", "Score: " + score + " | Level reached: " + level, "#ef4444");
    }

    private void victory() {
        inGame = false;
        isVictory = true;
        if (gameLoop != null) gameLoop.stop();
        statusLabel.setText("VICTORY! All 25 Levels Conquered!");
        statusLabel.setTextFill(Color.web("#22c55e"));

        showOverlay("VICTORY!", "You completed all 25 levels with " + score + " pts!", "#22c55e");
    }

    private void showOverlay(String titleText, String subText, String accentColor) {
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

        Button playAgain = GlowHelper.createNeonButton("Play Again", 150, 45);
        playAgain.setOnAction(e -> initGame());
        playAgain.setOnMouseClicked(e -> initGame());

        Button hubBtn = GlowHelper.createNeonButton("Arcade Hub", 150, 45);
        hubBtn.setOnAction(e -> onBackToArcade.run());
        hubBtn.setOnMouseClicked(e -> onBackToArcade.run());

        btns.getChildren().addAll(playAgain, hubBtn);
        overlayBox.getChildren().addAll(tLbl, sLbl, btns);
        overlayBox.setVisible(true);
    }

    private void updateScoreLabel() {
        String modeStr = isEndlessMode ? "Endless" : "Level " + level + "/25 (" + beersInLevel + "/" + BEERS_PER_LEVEL + ")";
        scoreLabel.setText("Score: " + score + " | " + modeStr + " | Length: " + snake.size());
    }

    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Subtle dark grid lines (pure black & dark neutral grey)
        gc.setStroke(Color.web("#141414"));
        gc.setLineWidth(1);
        for (int c = 0; c <= cols; c++) gc.strokeLine(c * tileSize, 0, c * tileSize, rows * tileSize);
        for (int r = 0; r <= rows; r++) gc.strokeLine(0, r * tileSize, cols * tileSize, r * tileSize);

        // Obstacles (black square with neon red border & Tic Tac Toe 'X')
        for (Point op : obstacles) {
            double ox = op.x * tileSize + 2;
            double oy = op.y * tileSize + 2;
            double sz = tileSize - 4;
            gc.setFill(Color.BLACK);
            gc.fillRect(ox, oy, sz, sz);
            gc.setStroke(Color.web("#ef4444"));
            gc.setLineWidth(2);
            gc.strokeRoundRect(ox, oy, sz, sz, 3, 3);
            gc.setLineWidth(1.5);
            gc.strokeLine(ox + 6, oy + 6, ox + sz - 6, oy + sz - 6);
            gc.strokeLine(ox + sz - 6, oy + 6, ox + 6, oy + sz - 6);
        }

        // Beer Mug (Food)
        if (beerPoint != null) {
            if (beerImg != null && !beerImg.isError()) {
                gc.drawImage(beerImg, beerPoint.x * tileSize + 1, beerPoint.y * tileSize + 1, tileSize - 2, tileSize - 2);
            } else {
                gc.setFill(Color.web("#eab308"));
                gc.fillOval(beerPoint.x * tileSize + 4, beerPoint.y * tileSize + 4, tileSize - 8, tileSize - 8);
            }
        }

        // Broccoli
        for (Point bp : broccoliPoints) {
            if (broccoliImg != null && !broccoliImg.isError()) {
                gc.drawImage(broccoliImg, bp.x * tileSize + 1, bp.y * tileSize + 1, tileSize - 2, tileSize - 2);
            } else {
                gc.setFill(Color.web("#10b981"));
                gc.fillOval(bp.x * tileSize + 4, bp.y * tileSize + 4, tileSize - 8, tileSize - 8);
            }
        }

        // Snake Body (crisp black square with neon border, minimal 3px rounding)
        for (int i = 0; i < snake.size(); i++) {
            Point p = snake.get(i);
            double px = p.x * tileSize + 2;
            double py = p.y * tileSize + 2;
            double sz = tileSize - 4;

            gc.setFill(Color.BLACK);
            gc.fillRect(px, py, sz, sz);

            if (i == 0) {
                // Head: bright neon green border + eye dots
                gc.setStroke(Color.web("#4ade80"));
                gc.setLineWidth(2.5);
                gc.strokeRoundRect(px, py, sz, sz, 3, 3);
                gc.setFill(Color.WHITE);
                gc.fillOval(px + 6, py + 6, 4, 4);
                gc.fillOval(px + sz - 10, py + 6, 4, 4);
            } else {
                gc.setStroke(snakeColor);
                gc.setLineWidth(2);
                gc.strokeRoundRect(px, py, sz, sz, 3, 3);
            }
        }
    }
}
