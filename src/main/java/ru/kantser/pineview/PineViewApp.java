package ru.kantser.pineview;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.adapter.download.HttpDownloadAdapter;
import ru.kantser.pineview.adapter.embedding.MockEmbeddingAdapter;
import ru.kantser.pineview.adapter.http.OkHttpHealthChecker;
import ru.kantser.pineview.adapter.persistence.JsonConfigAdapter;
import ru.kantser.pineview.adapter.pinecone.PineconeApiAdapter;
import ru.kantser.pineview.adapter.release.GitHubReleaseAdapter;
import ru.kantser.pineview.adapter.version.MavenVersionAdapter;
import ru.kantser.pineview.domain.usecase.CheckForUpdatesUseCase;
import ru.kantser.pineview.domain.usecase.RecordService;
import ru.kantser.pineview.ui.MainWindow;

public class PineViewApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(PineViewApp.class);

    private AppContext appContext;

    @Override
    public void start(Stage stage) throws Exception {
        log.info("[PineViewApp] Initializing application context...");

// 1. Адаптеры
        var configPort = new JsonConfigAdapter();
        var httpHealthChecker = new OkHttpHealthChecker();
        var pineconeAdapter = new PineconeApiAdapter();
        var embeddingAdapter = new MockEmbeddingAdapter();

        // Адаптеры для обновлений
        var versionPort = new MavenVersionAdapter();
        var releasePort = new GitHubReleaseAdapter();
        var downloadPort = new HttpDownloadAdapter();

        // 2. Сервисы
        var recordService = new RecordService(pineconeAdapter, embeddingAdapter);
        var checkForUpdatesUseCase = new CheckForUpdatesUseCase(versionPort, releasePort, downloadPort);

        // 3. Контекст
        appContext = new AppContext(
                configPort,
                httpHealthChecker,
                pineconeAdapter,
                recordService,
                checkForUpdatesUseCase
        );

        // 4. Загрузка главного окна с внедрением зависимостей
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/kantser/pineview/main-view.fxml"));

        // МАГИЯ DI: Мы говорим загрузчику, как создавать контроллеры.
        // Если запрашивается MainWindow, мы вызываем его конструктор с параметром appContext.
        loader.setControllerFactory(controllerClass -> {
            try {
                if (controllerClass == MainWindow.class) {
                    return new MainWindow(appContext);
                }
                // Для других контроллеров можно добавить условия
                // или использовать дефолтный конструктор
                return controllerClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                log.error("Failed to create controller: {}", controllerClass.getName(), e);
                throw new RuntimeException(e);
            }
        });

        Parent root = loader.load();
        MainWindow controller = loader.getController(); // Получаем созданный контроллер

        // 5. Настройка сцены
        Scene scene = new Scene(root, 900, 550);
        scene.getStylesheets().add(getClass().getResource("/ru/kantser/pineview/styles.css").toExternalForm());

        stage.setTitle("🌲 Elephant Pine View - " + versionPort.getCurrentVersion());
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(500);

        // Обработка закрытия
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