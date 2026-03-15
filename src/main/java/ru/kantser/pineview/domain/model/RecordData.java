package ru.kantser.pineview.domain.model;

import java.util.Map;

public record RecordData(String id, float[] vector, Map<String, Object> metadata) {}