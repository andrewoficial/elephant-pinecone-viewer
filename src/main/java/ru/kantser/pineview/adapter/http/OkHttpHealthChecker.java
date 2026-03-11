package ru.kantser.pineview.adapter.http;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.domain.model.HealthReport;
import ru.kantser.pineview.domain.model.ServiceStatus;
import ru.kantser.pineview.domain.port.HealthCheckPort;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class OkHttpHealthChecker implements HealthCheckPort {
    private static final Logger log = LoggerFactory.getLogger(OkHttpHealthChecker.class);

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();

    public OkHttpHealthChecker() {
        log.info("[OkHttpHealthChecker] [constructor] - Initializing HTTP health checker with timeout: 5s");
    }

    @Override
    public CompletableFuture<HealthReport> checkHealth(String serviceName, String url) {
        log.debug("[OkHttpHealthChecker] [checkHealth] - Checking health for {} at URL: {}", serviceName, url);

        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            try {
                Request request = new Request.Builder().url(url).head().build();
                log.debug("[OkHttpHealthChecker] [checkHealth] - Sending HEAD request to: {}", url);

                try (Response response = client.newCall(request).execute()) {
                    long responseTime = System.currentTimeMillis() - start;
                    boolean isOk = response.isSuccessful() && response.code() < 400;

                    log.debug("[OkHttpHealthChecker] [checkHealth] - Response received: code={}, time={}ms, successful={}",
                            response.code(), responseTime, isOk);

                    HealthReport report = new HealthReport(
                            serviceName,
                            url,
                            isOk ? ServiceStatus.ONLINE : ServiceStatus.OFFLINE,
                            responseTime,
                            "HTTP " + response.code()
                    );

                    if (isOk) {
                        log.info("[OkHttpHealthChecker] [checkHealth] - {} is ONLINE ({}ms)", serviceName, responseTime);
                    } else {
                        log.warn("[OkHttpHealthChecker] [checkHealth] - {} is OFFLINE: HTTP {}", serviceName, response.code());
                    }

                    return report;
                }
            } catch (IOException e) {
                long responseTime = System.currentTimeMillis() - start;
                log.error("[OkHttpHealthChecker] [checkHealth] - {} check failed: {} ({}ms)",
                        serviceName, e.getMessage(), responseTime, e);

                return new HealthReport(
                        serviceName,
                        url,
                        ServiceStatus.OFFLINE,
                        responseTime,
                        e.getMessage()
                );
            }
        });
    }
}