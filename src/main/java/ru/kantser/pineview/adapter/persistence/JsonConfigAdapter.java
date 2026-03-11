package ru.kantser.pineview.adapter.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.domain.port.ConfigPort;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JsonConfigAdapter implements ConfigPort {
    private static final Logger log = LoggerFactory.getLogger(JsonConfigAdapter.class);

    private static final String CONFIG_FILE = "config.json";
    private final ObjectMapper mapper = new ObjectMapper();
    private final File file;

    public JsonConfigAdapter() {
        log.info("[JsonConfigAdapter] [constructor] - Initializing with config file: {}", CONFIG_FILE);
        this.file = new File(CONFIG_FILE);
    }

    @Override
    public void save(String key, String value) {
        log.debug("[JsonConfigAdapter] [save] - Saving config: {} = {}", key, value);

        Map<String, String> config = loadAll();
        config.put(key, value);
        try {
            mapper.writeValue(file, config);
            log.info("[JsonConfigAdapter] [save] - Config saved successfully: {} = {}", key, value);
        } catch (IOException e) {
            log.error("[JsonConfigAdapter] [save] - Failed to save config: {}", e.getMessage(), e);
        }
    }

    @Override
    public Optional<String> load(String key) {
        log.debug("[JsonConfigAdapter] [load] - Loading config for key: {}", key);

        Optional<String> value = Optional.ofNullable(loadAll().get(key));

        if (value.isPresent()) {
            log.debug("[JsonConfigAdapter] [load] - Found value for key {}: {}", key, value.get());
        } else {
            log.debug("[JsonConfigAdapter] [load] - No value found for key: {}", key);
        }

        return value;
    }

    @Override
    public Map<String, String> loadAll() {
        log.debug("[JsonConfigAdapter] [loadAll] - Loading all configs from file: {}", CONFIG_FILE);

        if (!file.exists()) {
            log.info("[JsonConfigAdapter] [loadAll] - Config file does not exist, returning empty map");
            return new HashMap<>();
        }

        try {
            Map<String, String> config = mapper.readValue(file, Map.class);
            log.info("[JsonConfigAdapter] [loadAll] - Loaded {} config entries from file", config.size());
            return config;
        } catch (IOException e) {
            log.error("[JsonConfigAdapter] [loadAll] - Failed to load config file: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
}