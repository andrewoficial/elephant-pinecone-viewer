package ru.kantser.pineview;

import ru.kantser.pineview.adapter.pinecone.PineconeApiAdapter;
import ru.kantser.pineview.domain.port.ConfigPort;
import ru.kantser.pineview.domain.port.HealthCheckPort;
import ru.kantser.pineview.domain.usecase.CheckForUpdatesUseCase;
import ru.kantser.pineview.domain.usecase.RecordService;

/**
 * Контейнер зависимостей (Composition Root).
 * Создается один раз при старте приложения.
 */
public record AppContext(
        ConfigPort configPort,
        HealthCheckPort httpHealthChecker,
        PineconeApiAdapter pineconeAdapter,
        RecordService recordService,
        CheckForUpdatesUseCase checkForUpdatesUseCase
) {
}