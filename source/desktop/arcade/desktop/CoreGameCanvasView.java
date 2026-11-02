package arcade.desktop;

import arcade.Game;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

/**
 * JavaFX View Adapter hosting core decoupled arcade.Game engines via JavaFxGfx port.
 * Proves Hexagonal Architecture bridging between JavaFX and arcade.core.
 */
public class CoreGameCanvasView extends Canvas {

    private final Game game;
    private final JavaFxGfx gfx;
    private final AnimationTimer timer;
    private long lastTime = 0;

    public CoreGameCanvasView(Game game, double width, double height) {
        super(width, height);
        this.game = game;
        this.gfx = new JavaFxGfx(getGraphicsContext2D());

        this.timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }
                float dt = (now - lastTime) / 1_000_000_000f;
                lastTime = now;

                GraphicsContext gc = getGraphicsContext2D();
                gc.clearRect(0, 0, getWidth(), getHeight());
                game.frame(gfx, dt, (float) getWidth(), (float) getHeight());
            }
        };
    }

    public void start() {
        game.start();
        timer.start();
    }

    public void stop() {
        timer.stop();
        game.stop();
    }

    public Game getGame() {
        return game;
    }
}
