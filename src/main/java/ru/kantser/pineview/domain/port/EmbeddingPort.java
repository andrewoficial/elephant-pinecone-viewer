package ru.kantser.pineview.domain.port;

public interface EmbeddingPort {
    float[] generateEmbedding(String text);
}