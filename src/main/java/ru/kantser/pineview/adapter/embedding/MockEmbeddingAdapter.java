package ru.kantser.pineview.adapter.embedding;

import ru.kantser.pineview.domain.port.EmbeddingPort;

public class MockEmbeddingAdapter implements EmbeddingPort {
    @Override
    public float[] generateEmbedding(String text) {
        int dimension = 2048; // Размерность вашего индекса
        int hash = text.hashCode();
        float[] vector = new float[dimension];
        java.util.Random rng = new java.util.Random(hash);
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) (rng.nextGaussian() * 0.01);
        }
        return vector;
    }
}