package ru.kantser.pineview.domain.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.domain.model.HealthReport;
import ru.kantser.pineview.domain.model.ServiceStatus;
import ru.kantser.pineview.domain.port.HealthCheckPort;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class HealthMonitorService {
    private static final Logger log = LoggerFactory.getLogger(HealthMonitorService.class);

    // Dependencies
    private final HealthCheckPort healthChecker;
    private final Map<String, String> servicesToMonitor;
    private final Consumer<HealthReport> onStatusUpdate;

    private ScheduledExecutorService scheduler;
    private final long checkIntervalSeconds;

    public HealthMonitorService(HealthCheckPort healthChecker,
                                Map<String, String> servicesToMonitor,
                                Consumer<HealthReport> onStatusUpdate,
                                long checkIntervalSeconds) {
        log.info("[HealthMonitorService] [constructor] - Initializing with interval: {} seconds, services: {}",
                checkIntervalSeconds, servicesToMonitor.size());

        this.healthChecker = healthChecker;
        this.servicesToMonitor = servicesToMonitor;
        this.onStatusUpdate = onStatusUpdate;
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    public void start() {
        log.info("[HealthMonitorService] [start] - Starting health monitor service");

        scheduler = Executors.newScheduledThreadPool(3);

        // Проверяем сразу при старте
        log.debug("[HealthMonitorService] [start] - Performing initial health check");
        checkAllNow();

        // И потом по расписанию
        scheduler.scheduleAtFixedRate(
                this::checkAllNow,
                checkIntervalSeconds,
                checkIntervalSeconds,
                TimeUnit.SECONDS
        );

        log.info("[HealthMonitorService] [start] - Scheduled periodic checks every {} seconds", checkIntervalSeconds);
    }

    public void stop() {
        log.info("[HealthMonitorService] [stop] - Stopping health monitor service");

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    log.warn("[HealthMonitorService] [stop] - Scheduler didn't terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void checkAllNow() {
        log.debug("[HealthMonitorService] [checkAllNow] - Starting health check for {} services", servicesToMonitor.size());

        for (Map.Entry<String, String> entry : servicesToMonitor.entrySet()) {
            String serviceName = entry.getKey();
            String serviceUrl = entry.getValue();

            log.debug("[HealthMonitorService] [checkAllNow] - Checking service: {} at URL: {}", serviceName, serviceUrl);

            healthChecker.checkHealth(serviceName, serviceUrl)
                    .thenAccept(report -> {
                        log.debug("[HealthMonitorService] [checkAllNow] - Health check completed for {}: {}",
                                serviceName, report.getStatus());
                        onStatusUpdate.accept(report);
                    })
                    .exceptionally(ex -> {
                        log.warn("[HealthMonitorService] [checkAllNow] - Health check failed for {}: {}",
                                serviceName, ex.getMessage(), ex);

                        onStatusUpdate.accept(new HealthReport(
                                serviceName,
                                serviceUrl,
                                ServiceStatus.OFFLINE,
                                -1,
                                "Error: " + ex.getMessage()
                        ));
                        return null;
                    });
        }

        log.debug("[HealthMonitorService] [checkAllNow] - All health checks initiated");
    }
}