package ru.kantser.pineview;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.adapter.download.HttpDownloadAdapter;
import ru.kantser.pineview.adapter.embedding.MockEmbeddingAdapter;
import ru.kantser.pineview.adapter.http.OkHttpHealthChecker;
import ru.kantser.pineview.adapter.input.jsonl.JsonlImportAdapter;
import ru.kantser.pineview.adapter.input.xml.XmlImportAdapter;
import ru.kantser.pineview.adapter.persistence.JsonConfigAdapter;
import ru.kantser.pineview.adapter.pinecone.PineconeApiAdapter;
import ru.kantser.pineview.adapter.release.GitHubReleaseAdapter;
import ru.kantser.pineview.adapter.version.MavenVersionAdapter;
import ru.kantser.pineview.domain.port.*;
import ru.kantser.pineview.domain.usecase.CheckForUpdatesUseCase;
import ru.kantser.pineview.domain.usecase.ImportFromFileUseCase;
import ru.kantser.pineview.domain.usecase.SaveRecordUseCase;
import ru.kantser.pineview.ui.MainWindow;
import java.util.List;

public class PineViewApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(PineViewApp.class);

    private AppContext appContext;

    @Override
    public void start(Stage stage) throws Exception {
        log.info("[PineViewApp] Initializing application context...");

        ConfigPort configPort = new JsonConfigAdapter();
        HealthCheckPort healthCheckPort = new OkHttpHealthChecker();
        PineconeApiAdapter pineconeApiAdapter = new PineconeApiAdapter();
        RecordPort recordPort = pineconeApiAdapter;
        IndexPort indexPort = pineconeApiAdapter;
        ConnectionPort connectionPort = pineconeApiAdapter;

        var embeddingPort = new MockEmbeddingAdapter(); // Или реальный OpenAI

        var versionPort = new MavenVersionAdapter();
        var releasePort = new GitHubReleaseAdapter();
        var downloadPort = new HttpDownloadAdapter();

        var importParsers = List.of(
                new JsonlImportAdapter(),
                new XmlImportAdapter()
        );

        var saveRecordUseCase = new SaveRecordUseCase(recordPort, embeddingPort);
        var importFromFileUseCase = new ImportFromFileUseCase(importParsers, recordPort, embeddingPort);
        var checkForUpdatesUseCase = new CheckForUpdatesUseCase(versionPort, releasePort, downloadPort);

        appContext = new AppContext(
                configPort,
                healthCheckPort,
                recordPort,
                connectionPort,
                indexPort,
                saveRecordUseCase,
                importFromFileUseCase,
                checkForUpdatesUseCase
        );

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/kantser/pineview/main-view.fxml"));
        loader.setControllerFactory(controllerClass -> {
            try {
                if (controllerClass == MainWindow.class) {
                    return new MainWindow(appContext);
                }
                return controllerClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                log.error("Failed to create controller: {}", controllerClass.getName(), e);
                throw new RuntimeException(e);
            }
        });

        Parent root = loader.load();
        MainWindow controller = loader.getController(); // Получаем созданный контроллер

        Scene scene = new Scene(root, 900, 550);
        scene.getStylesheets().add(getClass().getResource("/ru/kantser/pineview/styles.css").toExternalForm());

        stage.setTitle("🌲 Elephant Pine View - " + versionPort.getCurrentVersion());
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(500);

        stage.setOnCloseRequest(e -> {
            log.info("[PineViewApp] Window close requested");
            if (controller != null) controller.cleanup();
        });

        log.info("[PineViewApp] Application window shown");
        stage.show();
    }


    @Override
    public void stop() throws Exception {
        log.info("[PineViewApp] [stop] - Stopping PineView application");
        super.stop();
        Platform.exit();
    }

    public static void main(String[] args) {
        log.info("[PineViewApp] [main] - Launching PineView application with {} arguments", args.length);
        launch(args);
    }
}