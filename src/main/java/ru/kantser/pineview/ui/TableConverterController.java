package ru.kantser.pineview.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TableConverterController {
    private static final Logger log = LoggerFactory.getLogger(TableConverterController.class);

    @FXML private TextArea inputArea;
    @FXML private TextArea outputArea;

    @FXML
    private void handleConvert() {
        String rawText = inputArea.getText();
        if (rawText == null || rawText.isBlank()) {
            outputArea.setText("// Вставьте текст таблицы слева");
            return;
        }

        try {
            // Логика конвертации
            String markdown = convertWordTableToMarkdown(rawText);
            outputArea.setText(markdown);
        } catch (Exception e) {
            log.error("Conversion failed", e);
            outputArea.setText("// Ошибка конвертации: " + e.getMessage());
        }
    }

    @FXML
    private void handleCopy() {
        String text = outputArea.getText();
        if (text != null && !text.isEmpty()) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
            log.info("Markdown copied to clipboard");
        }
    }

    @FXML
    private void handleClear() {
        inputArea.clear();
        outputArea.clear();
    }

    /**
     * Основная логика: Word при копировании таблицы разделяет ячейки табуляцией (\t),
     * а строки — переносом строки (\n).
     */
    private String convertWordTableToMarkdown(String raw) {
        String[] lines = raw.split("\\n");
        if (lines.length == 0) return "";

        StringBuilder md = new StringBuilder();
        int columnCount = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // Разделяем по табуляции
            String[] cells = line.split("\\t");
            
            // Определяем количество колонок по первой строке
            if (i == 0) {
                columnCount = cells.length;
            }

            // Формируем строку Markdown: | cell | cell |
            String mdRow = Arrays.stream(cells)
                    .map(cell -> " " + cell.trim() + " ")
                    .collect(Collectors.joining("|", "|", "|"));

            md.append(mdRow).append("\n");

            // После первой строки добавляем разделитель заголовка
            if (i == 0) {
                String separator = generateSeparator(columnCount);
                md.append(separator).append("\n");
            }
        }

        return md.toString();
    }

    private String generateSeparator(int count) {
        // Формирует строку вида: |---|---|---|
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < count; i++) {
            sb.append(" --- |");
        }
        return sb.toString();
    }
}