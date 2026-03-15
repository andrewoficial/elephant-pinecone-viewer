package ru.kantser.pineview.domain.port;

import ru.kantser.pineview.domain.model.HealthReport;
import java.util.concurrent.CompletableFuture;

public interface ConnectionPort {
    public void setApiKey(String apiKey);

    public CompletableFuture<HealthReport> checkHealth(String serviceName, String url);
}
