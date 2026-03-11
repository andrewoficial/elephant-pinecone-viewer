package ru.kantser.pineview.domain.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthReport {
    private static final Logger log = LoggerFactory.getLogger(HealthReport.class);

    private final String serviceName;
    private final String url;
    private final ServiceStatus status;
    private final long responseTimeMs;
    private final String message;

    public HealthReport(String serviceName, String url, ServiceStatus status,
                        long responseTimeMs, String message) {
        log.debug("[HealthReport] [constructor] - Creating health report: service={}, status={}, responseTime={}ms, message={}",
                serviceName, status, responseTimeMs, message);

        this.serviceName = serviceName;
        this.url = url;
        this.status = status;
        this.responseTimeMs = responseTimeMs;
        this.message = message;
    }

    // Getters
    public String getServiceName() { return serviceName; }
    public String getUrl() { return url; }
    public ServiceStatus getStatus() { return status; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public String getMessage() { return message; }
}