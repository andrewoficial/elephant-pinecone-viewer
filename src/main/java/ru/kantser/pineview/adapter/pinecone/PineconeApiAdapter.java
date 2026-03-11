package ru.kantser.pineview.adapter.pinecone;

import io.pinecone.clients.Pinecone;
import io.pinecone.clients.Index;
import org.openapitools.db_control.client.model.IndexList;
import org.openapitools.db_control.client.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.domain.model.HealthReport;
import ru.kantser.pineview.domain.model.ServiceStatus;
import ru.kantser.pineview.domain.port.HealthCheckPort;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PineconeApiAdapter implements HealthCheckPort {
    private static final Logger log = LoggerFactory.getLogger(PineconeApiAdapter.class);

    private Pinecone pinecone;
    private String currentApiKey;

    public void setApiKey(String apiKey) {
        log.info("[PineconeApiAdapter] [setApiKey] - Setting API key");
        this.currentApiKey = apiKey;
        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                // Новый API: Builder pattern
                this.pinecone = new Pinecone.Builder(apiKey).build();
                log.info("[PineconeApiAdapter] [setApiKey] - Pinecone client initialized (v6.1.0)");
            } catch (Exception e) {
                this.pinecone = null;
                log.error("[PineconeApiAdapter] [setApiKey] - Failed to init Pinecone: {}", e.getMessage(), e);
            }
        } else {
            log.warn("[PineconeApiAdapter] [setApiKey] - API key is null or empty");
        }
    }

    @Override
    public CompletableFuture<HealthReport> checkHealth(String serviceName, String url) {
        log.debug("[PineconeApiAdapter] [checkHealth] - Checking health for service: {}", serviceName);

        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            try {
                if (pinecone == null || currentApiKey == null || currentApiKey.isEmpty()) {
                    log.warn("[PineconeApiAdapter] [checkHealth] - No API key available");
                    return new HealthReport(serviceName, url, ServiceStatus.OFFLINE, -1, "No API key");
                }

                // Проверяем подключение: пробуем получить список индексов
                // В версии 6.1.0 используется IndexList из OpenAPI модели
                IndexList indexes = pinecone.listIndexes();
                long responseTime = System.currentTimeMillis() - start;

                int count = (indexes != null && indexes.getIndexes() != null)
                        ? indexes.getIndexes().size()
                        : 0;

                log.debug("[PineconeApiAdapter] [checkHealth] - Health check successful: {} indexes found in {}ms",
                        count, responseTime);

                return new HealthReport(
                        serviceName,
                        url,
                        ServiceStatus.ONLINE,
                        responseTime,
                        count + " indexes"
                );

            } catch (Exception e) {
                // Сетевая ошибка, таймаут и т.д.
                long responseTime = System.currentTimeMillis() - start;
                log.error("[PineconeApiAdapter] [checkHealth] - Health check failed: {}", e.getMessage(), e);

                return new HealthReport(
                        serviceName,
                        url,
                        ServiceStatus.OFFLINE,
                        responseTime,
                        e.getClass().getSimpleName() + ": " + e.getMessage()
                );
            }
        });
    }

    // Метод для получения списка индексов (для будущего UI)
    public CompletableFuture<List<org.openapitools.db_control.client.model.IndexModel>> fetchIndexes() {
        log.info("[PineconeApiAdapter] [fetchIndexes] - Fetching indexes");

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (pinecone == null) {
                    log.warn("[PineconeApiAdapter] [fetchIndexes] - Pinecone client not initialized");
                    return List.of();
                }

                log.debug("[PineconeApiAdapter] [fetchIndexes] - Fetching indexes from Pinecone API...");
                var indexList = pinecone.listIndexes();

                if (indexList == null || indexList.getIndexes() == null) {
                    log.info("[PineconeApiAdapter] [fetchIndexes] - Index list is empty or null");
                    return List.of();
                }

                var indexes = indexList.getIndexes();
                log.info("[PineconeApiAdapter] [fetchIndexes] - Retrieved {} indexes", indexes.size());
                return indexes;

            } catch (Exception e) {
                log.error("[PineconeApiAdapter] [fetchIndexes] - Error fetching indexes: {}", e.getMessage(), e);
                return List.of();
            }
        });
    }
}