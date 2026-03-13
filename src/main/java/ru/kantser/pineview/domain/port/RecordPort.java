package ru.kantser.pineview.domain.port;

import ru.kantser.pineview.domain.model.RecordData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface RecordPort {
    /**
     * Получить записи из индекса
     */
    CompletableFuture<List<RecordData>> fetchAllRecords(String indexName);

    /**
     * Сохранить (upsert) запись
     */
    CompletableFuture<Void> upsertRecord(String indexName, String id, float[] vector, Map<String, Object> metadata);

    /**
     * Удалить запись
     */
    CompletableFuture<Void> deleteRecord(String indexName, String id);
}