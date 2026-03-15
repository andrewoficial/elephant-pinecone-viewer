package ru.kantser.pineview.domain.model;

import java.util.Map;

public record ImportItem(String id, String text, Map<String, Object> metadata) {
    public boolean isValid() {
        return id != null && !id.isEmpty() && text != null && !text.isEmpty();
    }
}