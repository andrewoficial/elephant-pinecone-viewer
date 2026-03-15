package ru.kantser.pineview.adapter.input.jsonl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.domain.error.ImportException;
import ru.kantser.pineview.domain.model.ImportItem;
import ru.kantser.pineview.domain.model.ImportSource;
import ru.kantser.pineview.domain.port.ImportPort;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class JsonlImportAdapter implements ImportPort {

    private static final Logger log = LoggerFactory.getLogger(JsonlImportAdapter.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<ImportItem> parse(ImportSource source) throws ImportException {
        List<ImportItem> items = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(source.getStream(), source.getEncoding()))) {

            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    Map<String, Object> jsonMap = mapper.readValue(line, Map.class);
                    ImportItem item = mapToImportItem(jsonMap);

                    if (item.isValid()) {
                        items.add(item);
                    } else {
                        log.warn("Skipped invalid record at line {}: {}", lineNum, item);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse line {}: {}", lineNum, e.getMessage());
                    // Продолжаем парсить остальные строки — одна битая не ломает весь импорт
                }
            }
        } catch (IOException e) {
            // Перехватываем IOException и превращаем в наш доменный ImportException
            throw new ImportException("Failed to read source: " + source.getFileName(), e);
        }

        log.info("Parsed {} items from JSONL: {}", items.size(), source.getFileName());
        return items;
    }

    @Override
    public Set<String> supportedExtensions() {
        return Set.of("jsonl", "ndjson");
    }

    // Вынесли логику маппинга — так легче тестировать и читать
    private ImportItem mapToImportItem(Map<String, Object> json) {
        String id = (String) json.get("id");
        String text = (String) json.get("text");
        Map<String, Object> metadata = extractMetadata(json);
        return new ImportItem(id, text, metadata);
    }

    private Map<String, Object> extractMetadata(Map<String, Object> json) {
        if (json.containsKey("metadata") && json.get("metadata") instanceof Map) {
            return (Map<String, Object>) json.get("metadata");
        }
        // Если отдельного поля metadata нет — берём всё, кроме служебных полей
        Map<String, Object> metadata = new HashMap<>(json);
        metadata.remove("id");
        metadata.remove("text");
        return metadata;
    }
}