package io.mattw.jports.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class JportsUI extends Application {

    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        logger.debug("Starting Application");

        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            Parent parent = FXMLLoader.load(getClass().getResource("/io/mattw/jports/ui/Main.fxml"));

            Scene scene = new Scene(parent);
            scene.getStylesheets().add("/io/mattw/jports/ui/Styles.css");
            stage.setTitle("Port Scanner");
            stage.setScene(scene);
            stage.getIcons().add(new Image("/io/mattw/jports/ui/icon.png"));
            stage.setOnCloseRequest(we -> {
                logger.debug("Closing Application.");

                Platform.exit();
                System.exit(0);
            });
            stage.show();
        } catch (IOException e) {
            logger.fatal("Could not start application.", e);
            Platform.exit();
            System.exit(0);
        }
    }
}