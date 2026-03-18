package ru.kantser.pineview.adapter.pinecone;

import io.pinecone.clients.Pinecone;
import io.pinecone.clients.Index;
import org.openapitools.db_control.client.model.IndexList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.Constants;
import ru.kantser.pineview.domain.model.HealthReport;
import ru.kantser.pineview.domain.model.RecordData;
import ru.kantser.pineview.domain.model.ServiceStatus;
import ru.kantser.pineview.domain.port.ConnectionPort;
import ru.kantser.pineview.domain.port.IndexPort;
import ru.kantser.pineview.domain.port.RecordPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PineconeApiAdapter implements RecordPort, IndexPort, ConnectionPort {
    private static final Logger log = LoggerFactory.getLogger(PineconeApiAdapter.class);

    private Pinecone pinecone;
    private String currentApiKey;

    /**
     * Только для тестов: позволяет внедрить мок клиента
     */
    void setPineconeClient(Pinecone pinecone) {
        this.pinecone = pinecone;
        this.currentApiKey = "TEST_KEY"; // Имитируем, что ключ установлен
    }

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
    @Override
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

    @Override
    public CompletableFuture<Void> upsertTextRecord(String indexName, String id, String text, Map<String, Object> metadata) {
        return CompletableFuture.runAsync(() -> {
            log.info("Upserting text record {} to index {}", id, indexName);

            try {
                Index index = pinecone.getIndexConnection(indexName);

                // Собираем запись в формате, ожидаемом upsertRecords()
                Map<String, String> record = new HashMap<>();
                record.put("_id", id);
                // Поле для текста должно совпадать с field_map, указанным при создании индекса.
                // Обычно это "chunk_text" (как в примере) или "text". Уточните в настройках вашего индекса.
                record.put("text", text);

                for (Map.Entry<String, Object> stringObjectEntry : metadata.entrySet()) {
                    record.put(stringObjectEntry.getKey(), stringObjectEntry.getValue().toString());
                }

                // Вызываем upsertRecords для одной записи
                index.upsertRecords(
                        Constants.DEFAULT_INDEXES_NAMESPACE, // namespace (пустой, если не используете)
                        List.of(record)
                );

                log.info("Text upsert successful: {}", id);

            } catch (Exception e) {
                log.error("Text upsert failed for {}", id, e);
                throw new RuntimeException("Text upsert failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Получить все записи из индекса
     * ⚠️ Внимание: для больших индексов (>10000) нужна пагинация!
     */
    @Override
    public CompletableFuture<List<RecordData>> fetchAllRecords(String indexName) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Fetching all records from index: {}", indexName);

            try {
                if (pinecone == null) {
                    throw new IllegalStateException("Pinecone client not initialized");
                }

                Index index = pinecone.getIndexConnection(indexName);

                // 👇 Получаем размерность индекса
                int dimension = index.describeIndexStats().getDimension();
                float[] zeroVector = new float[dimension];

                //Изменил конвертацию
                List<Float> vectorList = new ArrayList<>();
                for (float v : zeroVector) {
                    vectorList.add(v);
                }
/*
                // 👇 Конвертируем float[] → List<Float> для параметра вектора
                List<Float> vectorList = java.util.Arrays.stream(zeroVector)
                        .boxed()
                        .collect(java.util.stream.Collectors.toList());
*/

                // 👇 Вызов query() с 9 параметрами (порядок из вашей подсказки IDE):
                // query(topK, vector, sparseIndices, sparseValues, id, namespace, filter, includeValues, includeMetadata)
                var response = index.query(
                        10000,                          // int topK
                        vectorList,                     // List<Float> vector
                        null,                           // List<Long> sparseIndices
                        null,                           // List<Float> sparseValues
                        null,                           // String id
                        "",                             // String namespace (пустой = дефолтный)
                        null,                           // com.google.protobuf.Struct filter
                        true,                           // boolean includeValues
                        true                            // boolean includeMetadata
                );

                var records = new ArrayList<RecordData>();

                // 👇 В прото-стиле: getMatchesCount() + getMatches(int index)
                var matchesList = response.getMatchesList();
                log.debug("Query returned {} matches", matchesList.size());

                for (var match : matchesList) {
                    // 👇 ID: просто получаем, в прото-объектах это всегда строка (может быть пустой)
                    String id = match.getId();
                    if (id == null || id.isEmpty()) {
                        log.warn("Found record with empty ID, skipping");
                        continue;
                    }

                    // 👇 Вектор: getValuesList() возвращает List<Float>
                    List<Float> valuesList = match.getValuesList();
                    float[] vector = new float[valuesList != null ? valuesList.size() : 0];
                    if (valuesList != null) {
                        for (int j = 0; j < vector.length; j++) {
                            vector[j] = valuesList.get(j);
                        }
                    }

                    // 👇 Метаданные: конвертируем protobuf Struct → Map
                    Map<String, Object> metadata = structToMap(match.getMetadata());

                    records.add(new RecordData(id, vector, metadata));
                }

                log.info("Fetched {} records from {}", records.size(), indexName);
                return records;

            } catch (Exception e) {
                log.error("Failed to fetch records from {}", indexName, e);
                throw new RuntimeException("Fetch failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Вспомогательный метод: конвертирует protobuf Struct в Java Map
     */
    private Map<String, Object> structToMap(com.google.protobuf.Struct struct) {
        if (struct == null) return java.util.Collections.emptyMap();

        Map<String, Object> result = new java.util.HashMap<>();
        for (var entry : struct.getFieldsMap().entrySet()) {
            var value = entry.getValue();
            // Простая конвертация: для продакшена добавьте обработку всех типов Value
            if (value.hasStringValue()) {
                result.put(entry.getKey(), value.getStringValue());
            } else if (value.hasNumberValue()) {
                result.put(entry.getKey(), value.getNumberValue());
            } else if (value.hasBoolValue()) {
                result.put(entry.getKey(), value.getBoolValue());
            } else if (value.hasStructValue()) {
                result.put(entry.getKey(), structToMap(value.getStructValue()));
            } else if (value.hasListValue()) {
                result.put(entry.getKey(), value.getListValue().getValuesList());
            } else {
                result.put(entry.getKey(), value.toString());
            }
        }
        return result;
    }

    /**
     * Добавить или обновить запись (upsert) — для одного вектора
     */
    public CompletableFuture<Void> upsertRecord(String indexName, String id, float[] vector, Map<String, Object> metadata) {
        return CompletableFuture.runAsync(() -> {
            log.info("Upserting record {} to index {}", id, indexName);

            try {
                Index index = pinecone.getIndexConnection(indexName);

                // 👇 Конвертируем float[] → List<Float>
                List<Float> vectorList = new ArrayList<>();
                for (float v : vector) {
                    vectorList.add(v);
                }

                // 👇 Конвертируем Map → protobuf Struct для метаданных
                com.google.protobuf.Struct structMetadata = mapToStruct(
                        metadata != null ? metadata : java.util.Collections.emptyMap()
                );

                // 👇 Вызов upsert() для ОДНОГО вектора:
                // (id, vector, sparseIndices, sparseValues, metadata, namespace)
                index.upsert(
                        id,                          // String id
                        vectorList,                  // List<Float> vector
                        null,                        // List<Long> sparseIndices (не используем)
                        null,                        // List<Float> sparseValues (не используем)
                        structMetadata,              // Struct metadata
                        ""                           // String namespace (пустой = дефолтный)
                );

                log.info("Upsert successful: {}", id);

            } catch (Exception e) {
                log.error("Upsert failed for {}", id, e);
                throw new RuntimeException("Upsert failed: " + e.getMessage(), e);
            }
        });
    }
    /**
     * Вспомогательный метод: конвертирует Java Map в protobuf Struct
     */
    private com.google.protobuf.Struct mapToStruct(Map<String, Object> map) {
        com.google.protobuf.Struct.Builder builder = com.google.protobuf.Struct.newBuilder();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            com.google.protobuf.Value.Builder valueBuilder = com.google.protobuf.Value.newBuilder();

            if (value == null) {
                valueBuilder.setNullValue(com.google.protobuf.NullValue.NULL_VALUE);
            } else if (value instanceof String) {
                valueBuilder.setStringValue((String) value);
            } else if (value instanceof Number) {
                valueBuilder.setNumberValue(((Number) value).doubleValue());
            } else if (value instanceof Boolean) {
                valueBuilder.setBoolValue((Boolean) value);
            } else if (value instanceof Map) {
                valueBuilder.setStructValue(mapToStruct((Map<String, Object>) value));
            } else if (value instanceof List) {
                valueBuilder.setListValue(com.google.protobuf.ListValue.newBuilder()
                        .addAllValues(((List<?>) value).stream()
                                .map(item -> {
                                    var vb = com.google.protobuf.Value.newBuilder();
                                    if (item == null) vb.setNullValue(com.google.protobuf.NullValue.NULL_VALUE);
                                    else if (item instanceof String) vb.setStringValue((String) item);
                                    else if (item instanceof Number) vb.setNumberValue(((Number) item).doubleValue());
                                    else if (item instanceof Boolean) vb.setBoolValue((Boolean) item);
                                    else vb.setStringValue(item.toString());
                                    return vb.build();
                                })
                                .collect(java.util.stream.Collectors.toList()))
                        .build());
            } else {
                // Fallback: всё остальное как строка
                valueBuilder.setStringValue(value.toString());
            }

            builder.putFields(key, valueBuilder.build());
        }

        return builder.build();
    }

    /**
     * Удалить запись по ID
     */
    public CompletableFuture<Void> deleteRecord(String indexName, String id) {
        return CompletableFuture.runAsync(() -> {
            log.info("Deleting record {} from index {}", id, indexName);

            try {
                var index = pinecone.getIndexConnection(indexName);
                /*
                public io.pinecone.proto.DeleteResponse delete(
                    java.util.List<String> ids,
                    boolean deleteAll,
                    String namespace,
                    com.google.protobuf.Struct filter
                )
                 */
                index.delete(List.of(id), false, Constants.DEFAULT_INDEXES_NAMESPACE, null);
                log.info("Delete successful: {}", id);
            } catch (Exception e) {
                log.error("Delete failed for {}", id, e);
                throw new RuntimeException("Delete failed: " + e.getMessage(), e);
            }
        });
    }
}