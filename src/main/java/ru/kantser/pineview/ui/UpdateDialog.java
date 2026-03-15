package ru.kantser.pineview.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.domain.model.ReleaseInfo;
import ru.kantser.pineview.domain.port.DownloadPort;
import ru.kantser.pineview.domain.usecase.CheckForUpdatesUseCase;
import java.io.IOException;

public class UpdateDialog extends Dialog<Void> {
    private static final Logger log = LoggerFactory.getLogger(UpdateDialog.class);

    @FXML private Button checkButton;
    @FXML private TextArea checkResultArea;
    @FXML private TextArea whatsNewArea;
    @FXML private Button downloadButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label downloadStatusLabel;
    @FXML private Label statusLabel;

    //dependencies
    private final CheckForUpdatesUseCase updatesUseCase;

    // === Внедрение зависимости через конструктор ===
    public UpdateDialog(Window owner, CheckForUpdatesUseCase updatesUseCase) {
        this.updatesUseCase = updatesUseCase; // Принимаем, не создаем

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/kantser/pineview/update-dialog.fxml"));
            loader.setController(this);
            DialogPane pane = loader.load();

            setDialogPane(pane);
            initOwner(owner);
            initModality(Modality.WINDOW_MODAL);
            setTitle("Обновление программы");
            setResizable(true);

            ButtonType closeButtonType = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
            getDialogPane().getButtonTypes().add(closeButtonType);

            progressBar.setProgress(0);
            downloadStatusLabel.setText("");
        } catch (IOException e) {
            log.error("Failed to load update dialog", e);
            throw new RuntimeException(e);
        }
    }


    @FXML
    private void handleCheck() {
        checkButton.setDisable(true);
        checkResultArea.setText("Проверка...");

        // Используем Task для асинхронности
        Task<Void> checkTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                boolean available = updatesUseCase.isUpdateAvailable();
                ReleaseInfo release = updatesUseCase.getLatestRelease();

                Platform.runLater(() -> {
                    if (available) {
                        checkResultArea.setText("Доступна версия: " + release.getVersion());
                        whatsNewArea.setText(release.getReleaseNotes());
                        downloadButton.setDisable(false);
                    } else {
                        checkResultArea.setText("У вас актуальная версия.");
                    }
                    checkButton.setDisable(false);
                });
                return null;
            }
        };

        checkTask.setOnFailed(e -> {
            log.error("Check failed", checkTask.getException());
            Platform.runLater(() -> {
                checkResultArea.setText("Ошибка: " + checkTask.getException().getMessage());
                checkButton.setDisable(false);
            });
        });

        new Thread(checkTask).start();
    }

    @FXML
    private void handleDownload() {
        log.info("Downloading update...");
        downloadButton.setDisable(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        downloadStatusLabel.setText("Загрузка...");

        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updatesUseCase.downloadUpdate(new DownloadPort.ProgressCallback() {
                    @Override
                    public void onProgress(long bytesRead, long totalBytes) {
                        if (totalBytes > 0) {
                            double progress = (double) bytesRead / totalBytes;
                            Platform.runLater(() -> {
                                progressBar.setProgress(progress);
                                downloadStatusLabel.setText(String.format("Загружено %d / %d KB", bytesRead / 1024, totalBytes / 1024));
                            });
                        }
                    }
                });

                Platform.runLater(() -> {
                    downloadStatusLabel.setText("Загрузка завершена! Файл сохранён.");
                });
                return null;
            }
        };

        downloadTask.setOnFailed(e -> {
            log.error("Download failed", downloadTask.getException());
            Platform.runLater(() -> {
                downloadStatusLabel.setText("Ошибка: " + downloadTask.getException().getMessage());
                downloadButton.setDisable(false);
                progressBar.setProgress(0);
            });
        });

        new Thread(downloadTask).start();
    }
}