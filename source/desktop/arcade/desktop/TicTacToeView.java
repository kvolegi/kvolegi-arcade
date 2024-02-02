package arcade.desktop;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class TicTacToeView {
    public static class GameSettings {
        private String opponent = "Computer";
        private String gameMode = "Line";
        private String playerSide = "X";
        private String computerSide = "O";
        private String difficulty = "Nightmare";
        private int winningScore = 5; // strictly 5 in a row per user requirement

        public String getOpponent() { return opponent; }
        public void setOpponent(String opponent) { this.opponent = opponent; }
        public String getGameMode() { return gameMode; }
        public void setGameMode(String gameMode) { this.gameMode = gameMode; }
        public String getPlayerSide() { return playerSide; }
        public void setPlayerSide(String playerSide) {
            this.playerSide = playerSide;
            this.computerSide = playerSide.equals("X") ? "O" : "X";
        }
        public String getComputerSide() { return computerSide; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public int getWinningScore() { return winningScore; }
        public void setWinningScore(int winningScore) { this.winningScore = winningScore; }
    }

    private static class CellState {
        String text;
        Color textColor;
        String style;
        Effect effect;
        CellState(String text, Color textColor, String style, Effect effect) {
            this.text = text;
            this.textColor = textColor;
            this.style = style;
            this.effect = effect;
        }
    }

    private final Stage stage;
    private final Runnable onBackToArcade;
    private final GameSettings gameSettings = new GameSettings();
    private Button[][] buttons;
    private boolean isXTurn = true;
    private boolean gameOver = false;
    private int boardSize = 5;
    private int scoreX = 0;
    private int scoreO = 0;
    private Timeline scoreModeTimer;
    private Label timerLabel;
    private Label scoreLabel;
    private int remainingTime = 60;
    private BorderPane mainContainer;

    public TicTacToeView(Stage stage, Runnable onBackToArcade) {
        this.stage = stage;
        this.onBackToArcade = onBackToArcade;
    }

    public static void show(Stage stage, Runnable onBackToArcade) {
        TicTacToeView view = new TicTacToeView(stage, onBackToArcade);
        view.showMainMenu();
    }

    public void showMainMenu() {
        VBox root = new VBox(35);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: black;");

        VBox titleBox = new VBox(10);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setMouseTransparent(true);

        Label title = new Label("TicTacToe!");
        title.setFont(GlowHelper.getFont(75, true));
        title.setTextFill(Color.BLACK);
        title.setEffect(GlowHelper.sharedGlowEffect);
        title.setMouseTransparent(true);
        titleBox.getChildren().add(title);

        Button startButton = GlowHelper.createNeonButton("Start Game", 420, 85);
        startButton.setOnAction(e -> {
            resetGame(true);
            showGameBoard();
        });

        Button settingsButton = GlowHelper.createNeonButton("Settings", 420, 85);
        settingsButton.setOnAction(e -> openSettingsWindow());

        Button backButton = GlowHelper.createNeonButton("Arcade Hub", 420, 85);
        backButton.setOnAction(e -> onBackToArcade.run());

        root.getChildren().addAll(titleBox, startButton, settingsButton, backButton);

        BorderPane container = new BorderPane();
        container.setStyle("-fx-background-color: black;");
        container.setCenter(root);

        BorderPane footer = new BorderPane();
        footer.setStyle("-fx-padding: 15;");
        footer.setLeft(GlowHelper.createWatermark());
        footer.setPickOnBounds(false);
        footer.setMouseTransparent(true);
        container.setBottom(footer);

        Scene scene = new Scene(container, 1050, 750);
        stage.setScene(scene);
    }

    private void showGameBoard() {
        mainContainer = new BorderPane();
        mainContainer.setStyle("-fx-background-color: black;");

        // Top bar
        HBox topPanel = new HBox(25);
        topPanel.setAlignment(Pos.CENTER);
        topPanel.setPadding(new Insets(15, 20, 10, 20));
        topPanel.setPickOnBounds(false);

        if (gameSettings.getGameMode().equals("Score")) {
            timerLabel = new Label("Time: " + remainingTime + "s");
            timerLabel.setFont(GlowHelper.getFont(20, true));
            timerLabel.setTextFill(Color.WHITE);
            timerLabel.setEffect(GlowHelper.sharedGlowEffect);
            timerLabel.setMouseTransparent(true);

            scoreLabel = new Label("X: " + scoreX + " | O: " + scoreO);
            scoreLabel.setFont(GlowHelper.getFont(20, true));
            scoreLabel.setTextFill(Color.WHITE);
            scoreLabel.setEffect(GlowHelper.sharedGlowEffect);
            scoreLabel.setMouseTransparent(true);

            topPanel.getChildren().addAll(timerLabel, scoreLabel);
        }

        Button restartButton = GlowHelper.createNeonButton("Restart", 150, 48);
        restartButton.setOnAction(e -> {
            resetGame(true);
            showGameBoard();
        });

        Button menuButton = GlowHelper.createNeonButton("Menu", 140, 48);
        menuButton.setOnAction(e -> {
            if (scoreModeTimer != null) scoreModeTimer.stop();
            showMainMenu();
        });

        topPanel.getChildren().addAll(restartButton, menuButton);
        mainContainer.setTop(topPanel);

        // Grid construction with adaptive sizing to fit ANY screen size perfectly
        buildAdaptiveGrid();

        // Footer
        BorderPane footer = new BorderPane();
        footer.setPadding(new Insets(10, 20, 15, 20));
        footer.setLeft(GlowHelper.createWatermark());
        footer.setPickOnBounds(false);
        footer.setMouseTransparent(true);
        mainContainer.setBottom(footer);

        Scene scene = new Scene(mainContainer, 1050, 750);

        // Dynamically adjust grid on window resize so it never overflows
        scene.widthProperty().addListener((obs, oldW, newW) -> buildAdaptiveGrid());
        scene.heightProperty().addListener((obs, oldH, newH) -> buildAdaptiveGrid());

        stage.setScene(scene);

        if (gameSettings.getOpponent().equals("Computer") && gameSettings.getComputerSide().equals("X") && isXTurn) {
            Platform.runLater(this::makeComputerMove);
        }
    }

    private void buildAdaptiveGrid() {
        if (mainContainer == null) return;

        double winW = stage.getScene() != null ? stage.getScene().getWidth() : (stage.getWidth() > 0 ? stage.getWidth() : 1050);
        double winH = stage.getScene() != null ? stage.getScene().getHeight() : (stage.getHeight() > 0 ? stage.getHeight() : 750);

        // Accommodate top bar and watermark footer with clean safety margin
        double maxGridH = Math.max(180, winH - 155);
        double maxGridW = Math.max(180, winW - 60);
        double maxBoardPx = Math.min(maxGridW, maxGridH);

        final int CELL_GAP = 3;
        double cellSize = (maxBoardPx - (boardSize - 1) * CELL_GAP) / (double) boardSize;
        cellSize = Math.max(18, Math.min(88, cellSize));

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-background-color: black;");
        grid.setHgap(CELL_GAP);
        grid.setVgap(CELL_GAP);

        Button[][] newButtons = new Button[boardSize][boardSize];
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                String existingText = (buttons != null && row < buttons.length && col < buttons[row].length) ? buttons[row][col].getText() : " ";
                Button button = GlowHelper.createNeonButton(existingText.isEmpty() ? " " : existingText, cellSize, cellSize);
                button.setPadding(Insets.EMPTY);
                button.setEllipsisString("");
                button.setAlignment(Pos.CENTER);
                button.setFont(GlowHelper.getFont(Math.max(12, cellSize * 0.44), true));

                int r = row, c = col;
                button.setOnAction(e -> {
                    if (!gameOver) {
                        boolean isPlayerTurn = (isXTurn && gameSettings.getPlayerSide().equals("X")) ||
                                (!isXTurn && gameSettings.getPlayerSide().equals("O"));

                        if (gameSettings.getOpponent().equals("Human") || isPlayerTurn) {
                            makeMove(button, r, c);
                        }
                    }
                });

                newButtons[row][col] = button;
                grid.add(button, col, row);
            }
        }
        buttons = newButtons;
        mainContainer.setCenter(grid);
    }

    private void makeMove(Button button, int row, int col) {
        if (button == null || !button.getText().trim().isEmpty() || gameOver) return;

        String currentSymbol = isXTurn ? "X" : "O";
        button.setPadding(Insets.EMPTY);
        button.setEllipsisString("");
        button.setText(currentSymbol);
        button.setTextFill(Color.WHITE);

        // Beautiful neon border flicker pulse (border-only flash per user request, background stays black!)
        String pulseColor = currentSymbol.equals("X") ? "#22c55e" : "#ef4444";
        button.setStyle("-fx-background-color: black; -fx-border-color: " + pulseColor + "; -fx-border-width: 3px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-padding: 0;");
        button.setEffect(new DropShadow(26, Color.web(pulseColor)));

        PauseTransition fadeHighlight = new PauseTransition(Duration.seconds(0.55));
        fadeHighlight.setOnFinished(e -> {
            button.setStyle("-fx-background-color: black; -fx-border-color: black; -fx-border-width: 3px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-padding: 0;");
            button.setEffect(GlowHelper.sharedGlowEffect);
        });
        fadeHighlight.play();

        if (("Line".equals(gameSettings.getGameMode()) || "Endless".equals(gameSettings.getGameMode())) && checkWin(row, col)) {
            gameOver = true;
            showWinScreen(currentSymbol);
            return;
        } else if (gameSettings.getGameMode().equals("Score")) {
            updateScoreForMove(row, col, currentSymbol);
            if (scoreModeTimer == null) startScoreModeTimer();
        }

        if (isBoardFull()) {
            gameOver = true;
            showWinScreen("Draw");
            return;
        }

        isXTurn = !isXTurn;

        expandBoard();

        if (gameSettings.getOpponent().equals("Computer") && !gameOver) {
            boolean computerShouldMove = (isXTurn && gameSettings.getComputerSide().equals("X")) ||
                    (!isXTurn && gameSettings.getComputerSide().equals("O"));
            if (computerShouldMove) {
                Platform.runLater(this::makeComputerMove);
            }
        }
    }

    private void makeComputerMove() {
        if (gameOver) return;
        try {
            String computerSymbol = gameSettings.getComputerSide();
            String playerSymbol = gameSettings.getPlayerSide();
            String diff = gameSettings.getDifficulty() == null ? "Nightmare" : gameSettings.getDifficulty();

            if ("Easy".equals(diff) && Math.random() < 0.6) {
                makeRandomMove();
                return;
            }

            int[] winningMove = findWinningMove(computerSymbol);
            if (winningMove != null && isValidEmpty(winningMove[0], winningMove[1])) {
                makeMove(buttons[winningMove[0]][winningMove[1]], winningMove[0], winningMove[1]);
                return;
            }

            int[] blockingMove = findWinningMove(playerSymbol);
            if (blockingMove != null && isValidEmpty(blockingMove[0], blockingMove[1])) {
                makeMove(buttons[blockingMove[0]][blockingMove[1]], blockingMove[0], blockingMove[1]);
                return;
            }

            if ("Hard".equals(diff) || "Nightmare".equals(diff)) {
                int[] bestMove = findBestTacticalMove(computerSymbol, playerSymbol);
                if (bestMove != null && isValidEmpty(bestMove[0], bestMove[1])) {
                    makeMove(buttons[bestMove[0]][bestMove[1]], bestMove[0], bestMove[1]);
                    return;
                }
            }

            int center = boardSize / 2;
            if (isValidEmpty(center, center)) {
                makeMove(buttons[center][center], center, center);
                return;
            }

            makeRandomMove();
        } catch (Throwable t) {
            makeRandomMove();
        }
    }

    private boolean isValidEmpty(int r, int c) {
        return buttons != null && r >= 0 && r < boardSize && c >= 0 && c < boardSize &&
                buttons[r][c] != null && buttons[r][c].getText().trim().isEmpty();
    }

    private void makeRandomMove() {
        if (gameOver || buttons == null) return;
        List<int[]> emptyCells = new ArrayList<>();
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (buttons[r][c] != null && buttons[r][c].getText().trim().isEmpty()) {
                    emptyCells.add(new int[]{r, c});
                }
            }
        }
        if (!emptyCells.isEmpty()) {
            int[] pick = emptyCells.get((int) (Math.random() * emptyCells.size()));
            makeMove(buttons[pick[0]][pick[1]], pick[0], pick[1]);
        } else {
            gameOver = true;
            showWinScreen("Draw");
        }
    }

    private boolean isBoardFull() {
        if (buttons == null) return false;
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (buttons[r][c] == null || buttons[r][c].getText().trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private int[] findWinningMove(String symbol) {
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (buttons[r][c].getText().trim().isEmpty()) {
                    buttons[r][c].setText(symbol);
                    boolean win = checkWin(r, c);
                    buttons[r][c].setText(" ");
                    if (win) return new int[]{r, c};
                }
            }
        }
        return null;
    }

    private int[] findBestTacticalMove(String computerSymbol, String playerSymbol) {
        int bestScore = Integer.MIN_VALUE;
        int[] bestCell = null;
        int center = boardSize / 2;

        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (buttons[r][c].getText().trim().isEmpty()) {
                    int compThreat = evaluateTacticalPoint(r, c, computerSymbol);
                    int playerThreat = evaluateTacticalPoint(r, c, playerSymbol);
                    int centerDist = Math.abs(r - center) + Math.abs(c - center);
                    int centerBonus = Math.max(0, 10 - centerDist * 2);
                    int score = (int)(compThreat * 1.15) + playerThreat + centerBonus;
                    if (score > bestScore) {
                        bestScore = score;
                        bestCell = new int[]{r, c};
                    }
                }
            }
        }
        return bestCell;
    }

    private int evaluateTacticalPoint(int row, int col, String symbol) {
        int total = 0;
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int[] dir : directions) {
            int count = 1;
            int openEnds = 0;

            int r = row + dir[0], c = col + dir[1];
            while (r >= 0 && r < boardSize && c >= 0 && c < boardSize && symbol.equals(buttons[r][c].getText())) {
                count++;
                r += dir[0]; c += dir[1];
            }
            if (r >= 0 && r < boardSize && c >= 0 && c < boardSize && buttons[r][c].getText().trim().isEmpty()) {
                openEnds++;
            }

            r = row - dir[0]; c = col - dir[1];
            while (r >= 0 && r < boardSize && c >= 0 && c < boardSize && symbol.equals(buttons[r][c].getText())) {
                count++;
                r -= dir[0]; c -= dir[1];
            }
            if (r >= 0 && r < boardSize && c >= 0 && c < boardSize && buttons[r][c].getText().trim().isEmpty()) {
                openEnds++;
            }

            int winTarget = Math.min(gameSettings.getWinningScore(), boardSize);
            if (count >= winTarget) total += 50000;
            else if (count == winTarget - 1 && openEnds == 2) total += 15000;
            else if (count == winTarget - 1 && openEnds == 1) total += 4000;
            else if (count == winTarget - 2 && openEnds == 2) total += 1200;
            else if (count == winTarget - 2 && openEnds == 1) total += 250;
            else if (count == 2 && openEnds == 2) total += 60;
            else if (count == 2 && openEnds == 1) total += 15;
        }
        return total;
    }

    private boolean checkWin(int row, int col) {
        if (buttons == null || row < 0 || row >= boardSize || col < 0 || col >= boardSize) return false;
        String currentSymbol = buttons[row][col].getText();
        if (currentSymbol.trim().isEmpty()) return false;
        int winCount = Math.min(gameSettings.getWinningScore(), boardSize);

        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int[] dir : directions) {
            int count = 1;
            int r = row + dir[0], c = col + dir[1];
            while (r >= 0 && r < boardSize && c >= 0 && c < boardSize && buttons[r][c] != null && currentSymbol.equals(buttons[r][c].getText())) {
                count++;
                r += dir[0]; c += dir[1];
            }
            r = row - dir[0]; c = col - dir[1];
            while (r >= 0 && r < boardSize && c >= 0 && c < boardSize && buttons[r][c] != null && currentSymbol.equals(buttons[r][c].getText())) {
                count++;
                r -= dir[0]; c -= dir[1];
            }
            if (count >= winCount) return true;
        }
        return false;
    }

    private void updateScoreForMove(int row, int col, String symbol) {
        int pts = countConsecutive(row, col, symbol);
        if ("X".equals(symbol)) scoreX += pts;
        else scoreO += pts;
        if (scoreLabel != null) scoreLabel.setText("X: " + scoreX + " | O: " + scoreO);
    }

    private int countConsecutive(int row, int col, String symbol) {
        int count = 0;
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int[] dir : directions) {
            int line = 1;
            int r = row + dir[0], c = col + dir[1];
            while (r >= 0 && r < boardSize && c >= 0 && c < boardSize && symbol.equals(buttons[r][c].getText())) {
                line++;
                r += dir[0]; c += dir[1];
            }
            r = row - dir[0]; c = col - dir[1];
            while (r >= 0 && r < boardSize && c >= 0 && c < boardSize && symbol.equals(buttons[r][c].getText())) {
                line++;
                r -= dir[0]; c -= dir[1];
            }
            if (line >= 5) count += 5;
            else if (line >= 3) count += line;
        }
        return count;
    }

    private void startScoreModeTimer() {
        if (scoreModeTimer != null) scoreModeTimer.stop();
        scoreModeTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingTime--;
            if (timerLabel != null) timerLabel.setText("Time: " + remainingTime + "s");
            if (remainingTime <= 0) {
                scoreModeTimer.stop();
                gameOver = true;
                showScoreWinScreen();
            }
        }));
        scoreModeTimer.setCycleCount(remainingTime);
        scoreModeTimer.play();
    }

    private void showWinScreen(String winner) {
        Stage winStage = new Stage();
        winStage.initModality(Modality.APPLICATION_MODAL);
        VBox winRoot = new VBox(25);
        winRoot.setAlignment(Pos.CENTER);
        winRoot.setStyle("-fx-background-color: black; -fx-padding: 30;");

        Label winLabel = new Label("Draw".equalsIgnoreCase(winner) ? "It's a Draw!" : (winner.contains("wins") || winner.contains("Draw") ? winner : winner + " Wins!"));
        winLabel.setFont(GlowHelper.getFont(45, true));
        winLabel.setTextFill(Color.WHITE);
        winLabel.setEffect(GlowHelper.sharedGlowEffect);

        Button playAgain = GlowHelper.createNeonButton("Play Again", 260, 70);
        playAgain.setOnAction(e -> {
            winStage.close();
            resetGame(true);
            showGameBoard();
        });

        Button menuButton = GlowHelper.createNeonButton("Return to Menu", 260, 70);
        menuButton.setOnAction(e -> {
            winStage.close();
            showMainMenu();
        });

        winRoot.getChildren().addAll(winLabel, playAgain, menuButton);
        Scene winScene = new Scene(winRoot, 600, 400);
        winStage.setScene(winScene);
        winStage.setTitle("Game Over");
        winStage.show();
    }

    private void showScoreWinScreen() {
        String res = (scoreX > scoreO) ? "X wins with " + scoreX + " pts!" :
                (scoreO > scoreX) ? "O wins with " + scoreO + " pts!" : "It's a draw (" + scoreX + " pts)!";
        showWinScreen(res);
    }

    // Exact original board expansion logic (+5 board expansion centered)
    private void expandBoard() {
        if (!isMoreThanHalfFilled() || boardSize >= 25) return;

        int newSize = boardSize + 5;
        CellState[][] oldBoard = saveCurrentBoardState();
        boardSize = newSize;

        buttons = null; // Important: Clear buttons reference before creating expanded grid
        buildAdaptiveGrid();
        transferOldMoves(oldBoard);

        if (gameSettings.getGameMode().equals("Score")) {
            updateScoreForAll();
        }
    }

    private boolean isMoreThanHalfFilled() {
        int filled = 0;
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (buttons != null && buttons[r][c] != null && !buttons[r][c].getText().trim().isEmpty()) {
                    filled++;
                }
            }
        }
        return filled > (boardSize * boardSize) / 2;
    }

    private CellState[][] saveCurrentBoardState() {
        CellState[][] oldBoard = new CellState[boardSize][boardSize];
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                Button btn = buttons[i][j];
                oldBoard[i][j] = new CellState(
                        btn.getText(),
                        (Color) btn.getTextFill(),
                        btn.getStyle(),
                        btn.getEffect()
                );
            }
        }
        return oldBoard;
    }

    private void transferOldMoves(CellState[][] oldBoard) {
        int oldSize = oldBoard.length;
        int offset = (boardSize - oldSize) / 2;

        for (int i = 0; i < oldSize; i++) {
            for (int j = 0; j < oldSize; j++) {
                if (!oldBoard[i][j].text.trim().isEmpty()) {
                    int newRow = i + offset;
                    int newCol = j + offset;
                    if (newRow < boardSize && newCol < boardSize) {
                        Button target = buttons[newRow][newCol];
                        CellState state = oldBoard[i][j];
                        target.setPadding(Insets.EMPTY);
                        target.setEllipsisString("");
                        target.setAlignment(Pos.CENTER);
                        target.setText(state.text);
                        target.setTextFill(state.textColor);
                        target.setStyle("-fx-background-color: black; -fx-border-color: black; -fx-border-width: 3px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-padding: 0;");
                        target.setEffect(state.effect);
                    }
                }
            }
        }
    }

    private void updateScoreForAll() {
        scoreX = recalcScore("X");
        scoreO = recalcScore("O");
        if (scoreLabel != null) {
            scoreLabel.setText("X: " + scoreX + " | O: " + scoreO);
        }
    }

    private int recalcScore(String symbol) {
        int total = 0;
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (buttons[r][c] != null && symbol.equals(buttons[r][c].getText())) {
                    total += countConsecutive(r, c, symbol);
                }
            }
        }
        return total;
    }

    private void resetGame(boolean full) {
        gameOver = false;
        isXTurn = true;
        boardSize = 5;
        buttons = null; // Important: Clear all old buttons so field is 100% clean
        scoreX = 0;
        scoreO = 0;
        remainingTime = 60;
        if (scoreModeTimer != null) {
            scoreModeTimer.stop();
            scoreModeTimer = null;
        }
    }

    // Settings window: NO ugly purple gradient! Pure black + white/neon borders
    public void openSettingsWindow() {
        Stage settingsStage = new Stage();
        settingsStage.initModality(Modality.APPLICATION_MODAL);
        VBox settingsRoot = new VBox(20);
        settingsRoot.setAlignment(Pos.CENTER);
        settingsRoot.setStyle("-fx-background-color: black; -fx-padding: 30;");

        String baseToggleStyle = "-fx-font-family: 'Comic Sans MS'; -fx-font-size: 15px; -fx-font-weight: bold; " +
                "-fx-background-color: black; -fx-text-fill: #94a3b8; " +
                "-fx-border-color: #334155; -fx-border-width: 2px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-padding: 10 24; -fx-cursor: hand;";

        String selectedToggleStyle = "-fx-font-family: 'Comic Sans MS'; -fx-font-size: 15px; -fx-font-weight: bold; " +
                "-fx-background-color: black; -fx-text-fill: white; " +
                "-fx-border-color: white; -fx-border-width: 2px; -fx-border-radius: 2px; -fx-background-radius: 2px; -fx-padding: 10 24; " +
                "-fx-effect: dropshadow(gaussian, white, 12, 0.45, 0, 0); -fx-cursor: hand;";

        // 1. Choose Opponent
        Label opponentLabel = new Label("Choose Opponent:");
        opponentLabel.setFont(GlowHelper.getFont(18, true));
        opponentLabel.setTextFill(Color.WHITE);

        ToggleGroup opponentGroup = new ToggleGroup();
        ToggleButton humanBtn = new ToggleButton("Human");
        ToggleButton compBtn = new ToggleButton("Computer");
        humanBtn.setToggleGroup(opponentGroup);
        compBtn.setToggleGroup(opponentGroup);

        humanBtn.setSelected(gameSettings.getOpponent().equals("Human"));
        compBtn.setSelected(gameSettings.getOpponent().equals("Computer"));
        humanBtn.setStyle(humanBtn.isSelected() ? selectedToggleStyle : baseToggleStyle);
        compBtn.setStyle(compBtn.isSelected() ? selectedToggleStyle : baseToggleStyle);

        humanBtn.selectedProperty().addListener((obs, oldV, isSel) -> humanBtn.setStyle(isSel ? selectedToggleStyle : baseToggleStyle));
        compBtn.selectedProperty().addListener((obs, oldV, isSel) -> compBtn.setStyle(isSel ? selectedToggleStyle : baseToggleStyle));

        HBox opponentBox = new HBox(20, humanBtn, compBtn);
        opponentBox.setAlignment(Pos.CENTER);

        // 2. Play as (side)
        Label sideLabel = new Label("Play as:");
        sideLabel.setFont(GlowHelper.getFont(18, true));
        sideLabel.setTextFill(Color.WHITE);

        ToggleGroup sideGroup = new ToggleGroup();
        ToggleButton xBtn = new ToggleButton("X");
        ToggleButton oBtn = new ToggleButton("O");
        xBtn.setToggleGroup(sideGroup);
        oBtn.setToggleGroup(sideGroup);
        xBtn.setSelected(gameSettings.getPlayerSide().equals("X"));
        oBtn.setSelected(gameSettings.getPlayerSide().equals("O"));
        xBtn.setStyle(xBtn.isSelected() ? selectedToggleStyle : baseToggleStyle);
        oBtn.setStyle(oBtn.isSelected() ? selectedToggleStyle : baseToggleStyle);

        xBtn.selectedProperty().addListener((obs, oldV, isSel) -> xBtn.setStyle(isSel ? selectedToggleStyle : baseToggleStyle));
        oBtn.selectedProperty().addListener((obs, oldV, isSel) -> oBtn.setStyle(isSel ? selectedToggleStyle : baseToggleStyle));

        HBox sideBox = new HBox(20, xBtn, oBtn);
        sideBox.setAlignment(Pos.CENTER);

        // 3. AI Difficulty
        Label diffLabel = new Label("AI Difficulty:");
        diffLabel.setFont(GlowHelper.getFont(18, true));
        diffLabel.setTextFill(Color.WHITE);

        ToggleGroup diffGroup = new ToggleGroup();
        ToggleButton easyBtn = new ToggleButton("Easy");
        ToggleButton normBtn = new ToggleButton("Normal");
        ToggleButton hardBtn = new ToggleButton("Hard");
        ToggleButton nightmareBtn = new ToggleButton("Nightmare (Max)");

        for (ToggleButton b : new ToggleButton[]{easyBtn, normBtn, hardBtn, nightmareBtn}) {
            b.setToggleGroup(diffGroup);
            b.setStyle(baseToggleStyle);
            b.selectedProperty().addListener((obs, oldV, isSel) -> b.setStyle(isSel ? selectedToggleStyle : baseToggleStyle));
        }

        String curDiff = gameSettings.getDifficulty();
        if ("Easy".equals(curDiff)) easyBtn.setSelected(true);
        else if ("Normal".equals(curDiff)) normBtn.setSelected(true);
        else if ("Hard".equals(curDiff)) hardBtn.setSelected(true);
        else nightmareBtn.setSelected(true);

        HBox diffBox = new HBox(12, easyBtn, normBtn, hardBtn, nightmareBtn);
        diffBox.setAlignment(Pos.CENTER);

        boolean isComp = compBtn.isSelected();
        sideLabel.setVisible(isComp); sideBox.setVisible(isComp);
        diffLabel.setVisible(isComp); diffBox.setVisible(isComp);

        compBtn.selectedProperty().addListener((obs, oldV, isSel) -> {
            sideLabel.setVisible(isSel); sideBox.setVisible(isSel);
            diffLabel.setVisible(isSel); diffBox.setVisible(isSel);
        });

        // 4. Game Mode (Both are on the Endless Board)
        Label modeLabel = new Label("Choose Game Mode:");
        modeLabel.setFont(GlowHelper.getFont(18, true));
        modeLabel.setTextFill(Color.WHITE);

        ToggleGroup modeGroup = new ToggleGroup();
        ToggleButton lineModeBtn = new ToggleButton("Line Mode (Endless)");
        ToggleButton scoreModeBtn = new ToggleButton("Score Mode (60s)");
        lineModeBtn.setToggleGroup(modeGroup);
        scoreModeBtn.setToggleGroup(modeGroup);

        String currentMode = gameSettings.getGameMode();
        scoreModeBtn.setSelected("Score".equals(currentMode));
        lineModeBtn.setSelected(!"Score".equals(currentMode));

        for (ToggleButton b : new ToggleButton[]{lineModeBtn, scoreModeBtn}) {
            b.setStyle(b.isSelected() ? selectedToggleStyle : baseToggleStyle);
            b.selectedProperty().addListener((obs, oldV, isSel) -> b.setStyle(isSel ? selectedToggleStyle : baseToggleStyle));
        }

        HBox modeBox = new HBox(20, lineModeBtn, scoreModeBtn);
        modeBox.setAlignment(Pos.CENTER);

        // Save Button
        Button saveBtn = GlowHelper.createNeonButton("Save Settings", 300, 60);
        saveBtn.setOnAction(e -> {
            gameSettings.setOpponent(humanBtn.isSelected() ? "Human" : "Computer");
            gameSettings.setGameMode(scoreModeBtn.isSelected() ? "Score" : "Endless");
            gameSettings.setWinningScore(5); // Strictly 5 in a row
            if (compBtn.isSelected()) {
                gameSettings.setPlayerSide(xBtn.isSelected() ? "X" : "O");
                if (easyBtn.isSelected()) gameSettings.setDifficulty("Easy");
                else if (normBtn.isSelected()) gameSettings.setDifficulty("Normal");
                else if (hardBtn.isSelected()) gameSettings.setDifficulty("Hard");
                else gameSettings.setDifficulty("Nightmare");
            }
            settingsStage.close();
        });

        settingsRoot.getChildren().addAll(
                opponentLabel, opponentBox,
                sideLabel, sideBox,
                diffLabel, diffBox,
                modeLabel, modeBox,
                new Region(), saveBtn,
                GlowHelper.createWatermark()
        );

        Scene scene = new Scene(settingsRoot, 820, 640);
        settingsStage.setScene(scene);
        settingsStage.setTitle("Settings");
        settingsStage.show();
    }
}
