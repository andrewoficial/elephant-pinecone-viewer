package ru.kantser.pineview.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import ru.kantser.pineview.AppContext;
import ru.kantser.pineview.domain.model.HealthReport;
import ru.kantser.pineview.domain.model.IndexDisplay;
import ru.kantser.pineview.domain.model.ServiceStatus;
import ru.kantser.pineview.domain.port.*;
import ru.kantser.pineview.domain.usecase.CheckForUpdatesUseCase;
import ru.kantser.pineview.domain.usecase.HealthMonitorService;
import ru.kantser.pineview.domain.usecase.ImportFromFileUseCase;
import ru.kantser.pineview.domain.usecase.SaveRecordUseCase;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainWindow {
    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    @FXML private TextField apiKeyField;
    @FXML private Button connectBtn;
    @FXML private Label apiStatusLabel;
    @FXML private VBox statusContainer; // Контейнер для индикаторов
    @FXML private Button recheckBtn;
    @FXML private Label nextCheckLabel;
    @FXML private TableView<IndexDisplay> indexTable;
    @FXML private TableColumn<IndexDisplay, String> colName;
    @FXML private TableColumn<IndexDisplay, Integer> colDim;
    @FXML private TableColumn<IndexDisplay, String> colMetric;
    @FXML private TableColumn<IndexDisplay, Long> colVectors;
    @FXML private TableColumn<IndexDisplay, String> colStatus;
    @FXML private Label indexCountLabel;
    @FXML private Label emptyStateLabel;
    @FXML private Button refreshIndexesBtn;

    private final AppContext ctx;

    //Dependencies
    private ConfigPort configPort;
    private SaveRecordUseCase saveRecordUseCase;
    private RecordPort recordPort;
    private ConnectionPort connectionPort;
    private IndexPort indexPort;
    private ImportFromFileUseCase importFromFileUseCase;
    private HealthCheckPort healthCheckPort;
    private CheckForUpdatesUseCase checkForUpdatesUseCase;

    private HealthMonitorService monitorService;
    private ScheduledExecutorService countdownScheduler;
    private volatile int secondsUntilNextCheck = 30;

    public MainWindow(AppContext ctx) {
        this.ctx = ctx;
        configPort = ctx.getConfigPort();
        saveRecordUseCase = ctx.getSaveRecordUseCase();
        recordPort = ctx.getRecordPort();
        importFromFileUseCase = ctx.getImportFromFileUseCase();
        healthCheckPort = ctx.getHealthCheckPort();
        checkForUpdatesUseCase = ctx.getCheckForUpdatesUseCase();
        connectionPort = ctx.getConnectionPort();
        indexPort = ctx.getIndexPort();

    }

    @FXML
    public void initialize() {
        log.info("[MainWindow] [initialize] - Initializing main window");

        // Загружаем сохранённый ключ
        configPort.load("apiKey").ifPresent(apiKeyField::setText);

        // Создаём индикаторы статусов
        StatusIndicator yaIndicator = new StatusIndicator("ya.ru");
        StatusIndicator pineconeWebIndicator = new StatusIndicator("pinecone.io");
        StatusIndicator pineconeApiIndicator = new StatusIndicator("Pinecone API");

        statusContainer.getChildren().addAll(yaIndicator, pineconeWebIndicator, pineconeApiIndicator);

        // Настраиваем монитор
        setupHealthMonitor(Map.of(
                "ya.ru", "https://ya.ru",
                "pinecone.io", "https://pinecone.io",
                "Pinecone API", "https://api.pinecone.io"
        ), report -> Platform.runLater(() -> {
            // Обновляем нужный индикатор
            switch (report.getServiceName()) {
                case "ya.ru" -> yaIndicator.updateStatus(report.getStatus(), formatDetail(report));
                case "pinecone.io" -> pineconeWebIndicator.updateStatus(report.getStatus(), formatDetail(report));
                case "Pinecone API" -> pineconeApiIndicator.updateStatus(report.getStatus(), formatDetail(report));
            }
        }));

        // Запускаем мониторинг
        if (monitorService != null) monitorService.start();
        startCountdown();
        setupIndexTable();
    }

    private void setupIndexTable() {
        log.info("[MainWindow] [setupIndexTable] - Setting up index table");

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDim.setCellValueFactory(new PropertyValueFactory<>("dimension"));
        colMetric.setCellValueFactory(new PropertyValueFactory<>("metric"));
        colVectors.setCellValueFactory(new PropertyValueFactory<>("vectorCount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Кастомный рендер статуса с цветом
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(status);
                    if (status.contains("Ready")) {
                        setStyle("-fx-text-fill: #a6e3a1;");
                    } else {
                        setStyle("-fx-text-fill: #fab387;");
                    }
                }
            }
        });

        indexTable.setRowFactory(tv -> {
            TableRow<IndexDisplay> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    IndexDisplay selected = row.getItem();
                    log.info("Double-clicked index: {}", selected.getName());
                    openIndexRecordsWindow(selected.getName());
                }
            });
            return row;
        });
    }

    private void openIndexRecordsWindow(String indexName) {
        try {
            log.info("Opening records window for index: {}", indexName);

            // 👇 Проверяем, что ресурс существует
            var fxmlUrl = getClass().getResource("/ru/kantser/pineview/index-records-view.fxml");
            if (fxmlUrl == null) {
                log.error("FXML file not found: index-records-view.fxml");
                log.error("Searched in: {}", getClass().getProtectionDomain().getCodeSource().getLocation());
                showAlert("Error", "UI file not found. Please rebuild project.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ru/kantser/pineview/index-records-view.fxml")
            );
            Parent root = loader.load();

            IndexRecordsWindow controller = loader.getController();

            controller.init(saveRecordUseCase, importFromFileUseCase, recordPort, indexName);

            Stage stage = new Stage();
            stage.setTitle("📦 Records: " + indexName);
            stage.setScene(new Scene(root, 1100, 700));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(apiStatusLabel.getScene().getWindow());
            stage.show();

            log.info("Opened records window for index: {}", indexName);

        } catch (Exception e) {
            log.error("Failed to open records window", e);
            showAlert("Error", "Could not open records: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefreshIndexes() {
        log.info("[MainWindow] [handleRefreshIndexes] - Manual refresh of indexes triggered");
        loadIndexes();
    }

    private void loadIndexes() {
        log.info("[MainWindow] [loadIndexes] - Loading indexes");

        if (recordPort == null) {
            log.error("[MainWindow] [loadIndexes] - pineconeAdapter is null!");
            return;
        }

        // Показываем пользователю, что идёт загрузка
        emptyStateLabel.setText("Loading indexes...");
        emptyStateLabel.setVisible(true);
        indexTable.setVisible(false);

        indexPort.fetchIndexes().thenAccept(indexes -> {
            log.info("[MainWindow] [loadIndexes] - fetchIndexes() completed: {} items",
                    indexes == null ? "null" : indexes.size());

            javafx.application.Platform.runLater(() -> {
                if (indexes == null || indexes.isEmpty()) {
                    log.info("[MainWindow] [loadIndexes] - No indexes to display");
                    indexTable.setVisible(false);
                    emptyStateLabel.setVisible(true);
                    emptyStateLabel.setText("No indexes found in your project");
                    indexCountLabel.setText("");
                } else {
                    log.info("[MainWindow] [loadIndexes] - Converting {} indexes for display", indexes.size());

                    try {
                        javafx.collections.ObservableList<IndexDisplay> displayList = indexes.stream()
                                .map(model -> {
                                    log.debug("[MainWindow] [loadIndexes] - Converting: {}", model.getName());
                                    return new IndexDisplay(model);
                                })
                                .collect(javafx.collections.FXCollections::observableArrayList,
                                        javafx.collections.ObservableList::add,
                                        javafx.collections.ObservableList::addAll);

                        indexTable.setItems(displayList);
                        if (!displayList.isEmpty()) {
                            var item = displayList.get(0);
                            log.info("🔍 TableView debug: name='{}', dimension={}, metric='{}', status='{}'",
                                    item.getName(), item.getDimension(), item.getMetric(), item.getStatus());
                        }
                        indexTable.setVisible(true);
                        emptyStateLabel.setVisible(false);
                        indexCountLabel.setText("(" + indexes.size() + ")");

                        log.info("[MainWindow] [loadIndexes] - Table updated with {} rows", displayList.size());

                    } catch (Exception e) {
                        log.error("[MainWindow] [loadIndexes] - Error creating IndexDisplay: {}", e.getMessage(), e);
                        emptyStateLabel.setText("Error: " + e.getMessage());
                    }
                }
                refreshIndexesBtn.setDisable(false);
            });

        }).exceptionally(ex -> {
            log.error("[MainWindow] [loadIndexes] - fetchIndexes() failed: {}", ex.getMessage(), ex);

            javafx.application.Platform.runLater(() -> {
                emptyStateLabel.setText("Error: " + ex.getMessage());
                emptyStateLabel.setStyle("-fx-text-fill: #f38ba8;");
                emptyStateLabel.setVisible(true);
            });
            return null;
        });
    }

    private void setupHealthMonitor(Map<String, String> services,
                                    java.util.function.Consumer<HealthReport> callback) {

        log.info("[MainWindow] [setupHealthMonitor] - Setting up health monitor for {} services", services.size());

        // Создаём "составной" адаптер: для Pinecone API используем спец. адаптер, для остальных — HTTP
        HealthCheckPort compositeChecker = (name, url) -> {
            if ("Pinecone API".equals(name)) {
                return healthCheckPort.checkHealth(name, url);
            }

            return healthCheckPort.checkHealth(name, url);
        };

        monitorService = new HealthMonitorService(compositeChecker, services, callback, 30);
    }

    @FXML
    private void handleConnect() {
        log.info("[MainWindow] [handleConnect] - Connecting with API key");

        String key = apiKeyField.getText().trim();
        if (key.isEmpty()) {
            log.warn("[MainWindow] [handleConnect] - Empty API key provided");
            showAlert("Enter API Key", "Please paste your Pinecone API key");
            return;
        }

        configPort.save("apiKey", key);
        connectionPort.setApiKey(key);

        // Визуально показываем процесс
        apiStatusLabel.setText("Checking API...");
        apiStatusLabel.setStyle("-fx-text-fill: #fab387;");
        connectBtn.setDisable(true);

        log.info("[MainWindow] [handleConnect] - Testing connection...");
        // 👇 Асинхронно проверяем подключение и ЗАТЕМ загружаем индексы
        healthCheckPort.checkHealth("Pinecone API", "https://api.pinecone.io")
                .thenAccept(report -> {
                    javafx.application.Platform.runLater(() -> {
                        connectBtn.setDisable(false);

                        if (report.getStatus() == ServiceStatus.ONLINE) {
                            // ✅ Успех!
                            apiStatusLabel.setText("✓ Connected");
                            apiStatusLabel.setStyle("-fx-text-fill: #a6e3a1;");

                            // 👇 КЛЮЧЕВОЙ ВЫЗОВ: загружаем индексы
                            loadIndexes();

                            log.info("[MainWindow] [handleConnect] - Connection OK, loading indexes...");
                            recheckBtn.setDisable(false);
                        } else {
                            // ❌ Ошибка
                            apiStatusLabel.setText("✗ " + report.getMessage());
                            apiStatusLabel.setStyle("-fx-text-fill: #f38ba8;");
                            showAlert("Connection Failed", report.getMessage());
                            log.warn("[MainWindow] [handleConnect] - Connection failed: {}", report.getMessage());
                            recheckBtn.setDisable(false);
                        }
                    });
                })
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        connectBtn.setDisable(false);
                        apiStatusLabel.setText("✗ Error");
                        showAlert("Error", ex.getMessage());
                    });
                    log.error("[MainWindow] [handleConnect] - Connection error: {}", ex.getMessage(), ex);
                    return null;
                });
    }

    @FXML
    private void handleRecheck() {
        log.info("[MainWindow] [handleRecheck] - Manual recheck triggered");

        // Сбрасываем таймер и запускаем проверку немедленно
        secondsUntilNextCheck = 30;
        updateCountdownLabel();

        if (monitorService != null) {
            // Принудительно запускаем проверку всех сервисов
            monitorService.checkAllNow();
        }
    }

    private void startCountdown() {
        log.info("[MainWindow] [startCountdown] - Starting countdown timer");

        // Останавливаем предыдущий таймер если есть
        if (countdownScheduler != null) {
            countdownScheduler.shutdownNow();
        }

        countdownScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true); // 👈 Поток не блокирует выход из JVM
            return t;
        });

        // Обновляем каждую секунду
        countdownScheduler.scheduleAtFixedRate(() -> {
            secondsUntilNextCheck--;
            if (secondsUntilNextCheck <= 0) {
                secondsUntilNextCheck = 30;
            }
            // Обновляем UI в JavaFX потоке
            javafx.application.Platform.runLater(this::updateCountdownLabel);
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void updateCountdownLabel() {
        nextCheckLabel.setText("Next check in: " + secondsUntilNextCheck + "s");
    }

    private void stopCountdown() {
        log.info("[MainWindow] [stopCountdown] - Stopping countdown timer");

        if (countdownScheduler != null) {
            countdownScheduler.shutdownNow();
        }
        nextCheckLabel.setText("");
    }

    private String formatDetail(HealthReport report) {
        if (report.getStatus() == ServiceStatus.CHECKING) return "checking...";
        if (report.getResponseTimeMs() > 0) {
            return report.getMessage() + " (" + report.getResponseTimeMs() + "ms)";
        }
        return report.getMessage();
    }

    private void showAlert(String title, String message) {
        log.warn("[MainWindow] [showAlert] - Showing alert: {} - {}", title, message);
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK).showAndWait();
    }

    public void cleanup() {
        log.info("[MainWindow] [cleanup] - Cleaning up resources...");
        stopCountdown();
        if (monitorService != null) {
            monitorService.stop();
        }
    }

    @FXML
    private void handleExit() {
        log.info("Exit requested from menu");
        Platform.exit();
    }

    @FXML
    private void handleCheckUpdates() {
        log.info("Check for updates requested");
        UpdateDialog dialog = new UpdateDialog(apiStatusLabel.getScene().getWindow(), checkForUpdatesUseCase);
        dialog.showAndWait();
    }

    @FXML
    private void handleOpenTableConverter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/kantser/pineview/table_converter.fxml"));

            if (loader.getLocation() == null) {
                throw new IllegalStateException("FXML file not found!");
            }

            VBox root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Table Converter");
            stage.setScene(new Scene(root));

            if (apiStatusLabel != null && apiStatusLabel.getScene() != null) {
                stage.initOwner(apiStatusLabel.getScene().getWindow());
            }

            stage.show();

        } catch (Exception e) {
            log.error("Failed to open Table Converter", e);
            showAlert("Error", "Could not open converter: " + e.getMessage());
        }
    }
}