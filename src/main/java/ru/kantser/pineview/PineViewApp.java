package ru.kantser.pineview;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.ui.MainWindow;

public class PineViewApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(PineViewApp.class);

    private MainWindow mainController;
    @Override
    public void start(Stage stage) throws Exception {
        log.info("[PineViewApp] [start] - Starting PineView application");

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("main-view.fxml")
        );

        mainController = loader.getController();

        log.debug("[PineViewApp] [start] - Loading FXML from: main-view.fxml");
        Scene scene = new Scene(loader.load(), 900, 550);

        stage.setTitle("🌲 PineView");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(500);

        stage.setOnCloseRequest(e -> {
            log.info("[PineViewApp] [start] - Window close requested");
            if (mainController != null) {
                mainController.cleanup();
            }
        });

        log.info("[PineViewApp] [start] - Main window created: size {}x{}", 900, 550);
        stage.show();
        log.info("[PineViewApp] [start] - Application window shown");
    }

    @Override
    public void stop() throws Exception {
        log.info("[PineViewApp] [stop] - Stopping PineView application");

        if (mainController != null) {
            log.debug("[PineViewApp] [stop] - Cleaning up main controller");
            mainController.cleanup();
        }

        log.info("[PineViewApp] [stop] - Application stopped");
        super.stop();

        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.exit(0); // Отключил для проверки и перестало закрываться опять
        }, "Shutdown-Guard").start();
    }

    public static void main(String[] args) {
        log.info("[PineViewApp] [main] - Launching PineView application with {} arguments", args.length);
        launch(args);
    }
}