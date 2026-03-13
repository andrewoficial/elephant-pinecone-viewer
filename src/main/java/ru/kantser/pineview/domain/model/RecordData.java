package ru.kantser.pineview.domain.model;

import java.util.Map;

/**
 * Модель данных записи
 */
public record RecordData(String id, float[] vector, Map<String, Object> metadata) {}