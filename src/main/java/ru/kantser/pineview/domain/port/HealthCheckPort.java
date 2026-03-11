package ru.kantser.pineview.domain.port;

import ru.kantser.pineview.domain.model.HealthReport;
import java.util.concurrent.CompletableFuture;

/**
 * Порт: "Умей проверить доступность сервиса по URL"
 * Адаптеры реализуют эту абстракцию (HTTP, gRPC, ping и т.д.)
 */
public interface HealthCheckPort {
    CompletableFuture<HealthReport> checkHealth(String serviceName, String url);
}