package ru.kantser.pineview.domain.port;

import java.util.Map;
import java.util.Optional;

/**
 * Порт: "Умей хранить настройки"
 */
public interface ConfigPort {
    void save(String key, String value);
    Optional<String> load(String key);
    Map<String, String> loadAll();
}