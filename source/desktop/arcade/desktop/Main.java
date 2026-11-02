package arcade.desktop;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        try {
            InputStream is = getClass().getResourceAsStream("/icon.png");
            if (is != null) {
                stage.getIcons().add(new Image(is));
            }
        } catch (Exception ignored) { }

        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar tb = Taskbar.getTaskbar();
                if (tb.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    InputStream isAwt = getClass().getResourceAsStream("/icon.png");
                    if (isAwt != null) {
                        BufferedImage img = ImageIO.read(isAwt);
                        if (img != null) {
                            tb.setIconImage(img);
                        }
                    }
                }
            }
        } catch (Throwable ignored) { }

        ArcadeHub.show(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
