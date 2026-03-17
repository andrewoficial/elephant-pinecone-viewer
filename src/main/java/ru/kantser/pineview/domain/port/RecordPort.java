package ru.kantser.pineview.domain.port;

import ru.kantser.pineview.domain.model.RecordData;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface RecordPort {
    CompletableFuture<List<RecordData>> fetchAllRecords(String indexName);

    CompletableFuture<Void> upsertRecord(String indexName, String id, float[] vector, Map<String, Object> metadata);


    CompletableFuture<Void> deleteRecord(String indexName, String id);
}