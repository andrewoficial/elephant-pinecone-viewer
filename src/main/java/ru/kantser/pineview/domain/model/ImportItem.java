package ru.kantser.pineview.domain.model;

import java.util.Map;

/**
 * Модель данных для импорта из JSONL
 */
public record ImportItem(String id, String text, Map<String, Object> metadata) {
    // Валидация основных полей
    public boolean isValid() {
        return id != null && !id.isEmpty() && text != null && !text.isEmpty();
    }
}