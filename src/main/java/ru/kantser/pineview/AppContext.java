package ru.kantser.pineview;

import ru.kantser.pineview.domain.port.*;
import ru.kantser.pineview.domain.usecase.*;

public class AppContext {
    // === Ports (интерфейсы) ===
    private final ConfigPort configPort;
    private final HealthCheckPort healthCheckPort;
    private final RecordPort recordPort;
    private final ConnectionPort connectionPort;
    private final IndexPort indexPort;

    // === UseCases (бизнес-сценарии) ===
    private final SaveRecordUseCase saveRecordUseCase;
    private final ImportFromFileUseCase importFromFileUseCase;
    private final CheckForUpdatesUseCase checkForUpdatesUseCase;

    public AppContext(
            ConfigPort configPort,
            HealthCheckPort healthCheckPort,
            RecordPort recordPort,
            ConnectionPort connectionPort,
            IndexPort indexPort,
            SaveRecordUseCase saveRecordUseCase,
            ImportFromFileUseCase importFromFileUseCase,
            CheckForUpdatesUseCase checkForUpdatesUseCase
    ) {
        this.configPort = configPort;
        this.healthCheckPort = healthCheckPort;
        this.recordPort = recordPort;
        this.saveRecordUseCase = saveRecordUseCase;
        this.importFromFileUseCase = importFromFileUseCase;
        this.checkForUpdatesUseCase = checkForUpdatesUseCase;
        this.indexPort = indexPort;
        this.connectionPort = connectionPort;
    }

    public ConfigPort getConfigPort() {
        return configPort;
    }

    public HealthCheckPort getHealthCheckPort() {
        return healthCheckPort;
    }

    public RecordPort getRecordPort() {
        return recordPort;
    }

    public SaveRecordUseCase getSaveRecordUseCase() {
        return saveRecordUseCase;
    }

    public ImportFromFileUseCase getImportFromFileUseCase() {
        return importFromFileUseCase;
    }

    public CheckForUpdatesUseCase getCheckForUpdatesUseCase() {
        return checkForUpdatesUseCase;
    }

    public ConnectionPort getConnectionPort() {
        return connectionPort;
    }

    public IndexPort getIndexPort() {
        return indexPort;
    }
}