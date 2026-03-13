package ru.kantser.pineview.adapter.input.jsonl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.domain.model.ImportItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonlImportAdapter {
    private static final Logger log = LoggerFactory.getLogger(JsonlImportAdapter.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public List<ImportItem> parse(File file) throws IOException {
        List<ImportItem> items = new ArrayList<>();
        int lineNum = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    // Парсим строку как Map
                    Map<String, Object> jsonMap = mapper.readValue(line, Map.class);

                    // Извлекаем поля
                    String id = (String) jsonMap.get("id");
                    String text = (String) jsonMap.get("text");
                    
                    // Метаданные могут быть отдельным полем или мы берем весь объект, удалив служебные поля
                    Map<String, Object> metadata = (Map<String, Object>) jsonMap.get("metadata");
                    if (metadata == null) {
                        metadata = new java.util.HashMap<>(jsonMap);
                        metadata.remove("id");
                        metadata.remove("text");
                    }

                    ImportItem item = new ImportItem(id, text, metadata);
                    
                    if (item.isValid()) {
                        items.add(item);
                    } else {
                        log.warn("Skipped invalid record at line {}: missing id or text", lineNum);
                    }

                } catch (Exception e) {
                    log.error("Failed to parse line {}: {}", lineNum, e.getMessage());
                    // Можно либо прервать импорт, либо продолжить. Здесь продолжаем.
                }
            }
        }
        log.info("Parsed {} valid items from file {}", items.size(), file.getName());
        return items;
    }
}