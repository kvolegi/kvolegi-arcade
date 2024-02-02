package arcade.desktop;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

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

        ArcadeHub.show(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
