package ru.kantser.pineview.domain.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.Constants;
import ru.kantser.pineview.domain.port.EmbeddingPort;
import ru.kantser.pineview.domain.port.RecordPort;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SaveRecordUseCase {
    private static final Logger log = LoggerFactory.getLogger(SaveRecordUseCase.class);

    // Dependencies
    private final RecordPort recordPort;
    private final EmbeddingPort embeddingPort;

    public SaveRecordUseCase(RecordPort recordPort, EmbeddingPort embeddingPort) {
        this.recordPort = recordPort;
        this.embeddingPort = embeddingPort;
    }

    public CompletableFuture<Void> save(String id, String text, Map<String, Object> metadata) {
        return saveToIndex("general_provisions", id, text, metadata);
        //_default_
    }

    public CompletableFuture<Void> saveToIndex(String indexName, String id, String text, Map<String, Object> metadata) {
        // Генерация эмбеддинга может быть дорогой — выполняем асинхронно
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        return embeddingPort.generateEmbedding(text);
                    } catch (Exception e) {
                        log.error("Failed to generate embedding for record {}", id, e);
                        throw new RuntimeException("Embedding generation failed", e);
                    }
                })
                .thenCompose(vector -> {
                    // Подготовка метаданных
                    Map<String, Object> finalMetadata = new HashMap<>(metadata != null ? metadata : Map.of());
                    finalMetadata.put("original_text", text);
                    finalMetadata.put("text", text);
                    finalMetadata.put("source", "manual-entry");
                    finalMetadata.put("saved_at", System.currentTimeMillis());

                    float[] def = new float[2048];
                    def[1] = 0.005F;
                    // Сохранение в векторную БД
                    return recordPort.upsertRecord(indexName, id, def, finalMetadata);
                })
                .exceptionally(ex -> {
                    log.error("Failed to save record {}: {}", id, ex.getMessage());
                    // Прокидываем ошибку дальше, чтобы UI мог показать алерт
                    throw new RuntimeException("Save failed: " + ex.getMessage(), ex);
                });
    }
}