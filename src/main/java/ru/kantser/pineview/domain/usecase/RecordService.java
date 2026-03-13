package ru.kantser.pineview.domain.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.adapter.input.jsonl.JsonlImportAdapter;
import ru.kantser.pineview.adapter.pinecone.PineconeApiAdapter;
import ru.kantser.pineview.domain.model.ImportItem;
import ru.kantser.pineview.domain.port.EmbeddingPort;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import ru.kantser.pineview.domain.port.RecordPort;

public class RecordService {
    private static final Logger log = LoggerFactory.getLogger(RecordService.class);

    private final RecordPort recordPort;
    private final EmbeddingPort embeddingPort;
    private final JsonlImportAdapter importAdapter;

    public RecordService(RecordPort recordPort, EmbeddingPort embeddingPort) {
        this.recordPort = recordPort;
        this.embeddingPort = embeddingPort;
        this.importAdapter = new JsonlImportAdapter();
    }

    public CompletableFuture<Void> saveRecord(String indexName, String id, String text, Map<String, Object> metadata) {
        float[] vector = embeddingPort.generateEmbedding(text);
        
        Map<String, Object> finalMetadata = new HashMap<>(metadata);
        finalMetadata.put("text", text);
        finalMetadata.put("source", "pineview-client");
        
        return recordPort.upsertRecord(indexName, id, vector, finalMetadata);
    }

    public CompletableFuture<ImportResult> importFromFile(String indexName, File file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ImportItem> items = importAdapter.parse(file);
                if (items.isEmpty()) {
                    return new ImportResult(0, 0, "No valid records found");
                }

                AtomicInteger successCount = new AtomicInteger(0);
                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (ImportItem item : items) {
                    float[] vector = embeddingPort.generateEmbedding(item.text());

                    Map<String, Object> finalMetadata = new HashMap<>(item.metadata());
                    finalMetadata.put("text", item.text());
                    finalMetadata.put("source", "jsonl-import");

                    CompletableFuture<Void> future = recordPort.upsertRecord(indexName, item.id(), vector, finalMetadata)
                            .thenRun(successCount::incrementAndGet)
                            .exceptionally(ex -> {
                                log.error("Failed to import record {}", item.id(), ex);
                                return null;
                            });
                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                return new ImportResult(successCount.get(), items.size(), "Success");

            } catch (Exception e) {
                log.error("Import failed", e);
                throw new RuntimeException("Import failed: " + e.getMessage(), e);
            }
        });
    }

    public static class ImportResult {
        public final int successCount;
        public final int total;
        public final String message;

        public ImportResult(int successCount, int total, String message) {
            this.successCount = successCount;
            this.total = total;
            this.message = message;
        }
    }
}