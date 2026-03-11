package ru.kantser.pineview.domain.model;

import javafx.beans.property.*;
import org.openapitools.db_control.client.model.IndexModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexDisplay {
    private static final Logger log = LoggerFactory.getLogger(IndexDisplay.class);

    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty dimension = new SimpleIntegerProperty();
    private final StringProperty metric = new SimpleStringProperty();
    private final LongProperty vectorCount = new SimpleLongProperty(-1); // -1 = недоступно
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty environment = new SimpleStringProperty();

    public IndexDisplay(IndexModel model) {
        // 👇 Защита от null модели
        if (model == null) {
            log.warn("[IndexDisplay] Received null model");
            this.name.set("unknown");
            this.status.set("⚠️ Error");
            return;
        }

        log.debug("Creating IndexDisplay for: {}", model.getName());

        // Name (обязательное поле в API, но проверяем на всякий случай)
        this.name.set(model.getName() != null ? model.getName() : "unnamed");

        // Dimension
        this.dimension.set(model.getDimension() != null ? model.getDimension() : 0);

        // Metric
        this.metric.set(model.getMetric() != null ? model.getMetric() : "cosine");

        // Status (вложенный объект)
        var apiStatus = model.getStatus();
        if (apiStatus != null && Boolean.TRUE.equals(apiStatus.getReady())) {
            this.status.set("✓ Ready");
        } else if (apiStatus != null) {
            this.status.set("⋯ Initializing");
        } else {
            this.status.set("❓ Unknown");
        }

        // Vector count — ⚠️ ВАЖНО:
        // В Pinecone API v6.1.0 метод listIndexes() НЕ возвращает approximateVectorCount.
        // Чтобы получить количество векторов, нужно делать отдельный запрос:
        //   pinecone.describeIndex(name).getUsage().getApproximateVectorCount()
        // Для таблицы оставляем -1 (неизвестно).
        // TODO: Добавить метод fetchVectorCount(String indexName) при необходимости
        this.vectorCount.set(-1);

        // Spec / Environment
        var spec = model.getSpec();
        this.environment.set(spec != null ? formatSpec(spec) : "serverless");

        log.debug("IndexDisplay created: name={}, dim={}, metric={}, status={}",
                name.get(), dimension.get(), metric.get(), status.get());
    }

    private String formatSpec(Object spec) {
        if (spec == null) return "unknown";
        try {
            return spec.toString()
                    .replaceAll("[{}\"']", "")
                    .replaceAll("\\s+", " ")
                    .substring(0, Math.min(25, spec.toString().length()));
        } catch (Exception e) {
            log.debug("Failed to format spec: {}", e.getMessage());
            return "unknown";
        }
    }

    // === Getters для PropertyValueFactory (имена должны совпадать!) ===
    public String getName() { return name.get(); }
    public int getDimension() { return dimension.get(); }
    public String getMetric() { return metric.get(); }
    public long getVectorCount() { return vectorCount.get(); }
    public String getStatus() { return status.get(); }
    public String getEnvironment() { return environment.get(); }

    // Property getters для binding
    public StringProperty nameProperty() { return name; }
    public IntegerProperty dimensionProperty() { return dimension; }
    public StringProperty metricProperty() { return metric; }
    public LongProperty vectorCountProperty() { return vectorCount; }
    public StringProperty statusProperty() { return status; }
}