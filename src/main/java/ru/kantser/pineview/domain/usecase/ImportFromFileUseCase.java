package ru.kantser.pineview.domain.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.Constants;
import ru.kantser.pineview.domain.error.ImportException;
import ru.kantser.pineview.domain.model.*;
import ru.kantser.pineview.domain.port.EmbeddingPort;
import ru.kantser.pineview.domain.port.ImportPort;
import ru.kantser.pineview.domain.port.RecordPort;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ImportFromFileUseCase {
    private static final Logger log = LoggerFactory.getLogger(ImportFromFileUseCase.class);

    // Dependencies
    private final List<ImportPort> parsers;
    private final RecordPort recordPort;
    private final EmbeddingPort embeddingPort;

    public ImportFromFileUseCase(List<ImportPort> parsers, RecordPort recordPort, EmbeddingPort embeddingPort) {
        this.parsers = parsers;
        this.recordPort = recordPort;
        this.embeddingPort = embeddingPort;
    }

    public ImportResult importFile(File file, String encoding) throws ImportException {
        try {
            ImportSource source = new ImportSource(
                    new FileInputStream(file),
                    file.getName(),
                    encoding != null ? encoding : "UTF-8"
            );

            ImportPort parser = findParserFor(source);
            if (parser == null) {
                throw new ImportException("Unsupported format: " + source.getExtension(), null);
            }

            List<ImportItem> items = parser.parse(source);
            int saved = saveItems(items);

            return new ImportResult(items.size(), saved, file.getName());

        } catch (FileNotFoundException e) {
            throw new ImportException("File not found: " + file.getName(), e);
        } catch (ExecutionException | InterruptedException e) {
            throw new ImportException("Failed to save records to index", e);
        }
    }

    public ImportResult importWithFormat(File file, String format, String encoding)
            throws ImportException {

        try {
            ImportPort parser = parsers.stream()
                    .filter(p -> p.supportedExtensions().contains(format.toLowerCase()))
                    .findFirst()
                    .orElseThrow(() -> new ImportException("No parser for format: " + format, null));

            ImportSource source = new ImportSource(
                    new FileInputStream(file),
                    file.getName(),
                    encoding != null ? encoding : "UTF-8"
            );

            List<ImportItem> items = parser.parse(source);
            int saved = saveItems(items);

            return new ImportResult(items.size(), saved, file.getName());

        } catch (FileNotFoundException e) {
            throw new ImportException("File not found: " + file.getName(), e);
        } catch (ExecutionException | InterruptedException e) {
            throw new ImportException("Failed to save records to index", e);
        }
    }

    private ImportPort findParserFor(ImportSource source) {
        String ext = source.getExtension();
        return parsers.stream()
                .filter(p -> p.supportedExtensions().contains(ext))
                .findFirst()
                .orElse(null);
    }

    private int saveItems(List<ImportItem> items) throws ExecutionException, InterruptedException {
        int saved = 0;
        for (ImportItem item : items) {
            try {
                RecordData record = convertToRecord(item);
                record.metadata().put("text", item.text());
                record.metadata().put("original_text", item.text());
                record.metadata().put("saved_at", System.currentTimeMillis());
                recordPort.upsertRecord(Constants.CURRENT_AREA, record.id(), record.vector(), record.metadata()).get();
                saved++;
            } catch (Exception e) {
                log.warn("Failed to save item {}: {}", item.id(), e.getMessage());
            }
        }
        return saved;
    }


    private RecordData convertToRecord(ImportItem item) {
        float[] vector = embeddingPort.generateEmbedding(item.text());

        Map<String, Object> metadata = new HashMap<>(item.metadata());
        metadata.put("original_text", item.text());

        return new RecordData(item.id(), vector, metadata);
    }
}