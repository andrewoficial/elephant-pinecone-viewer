package ru.kantser.pineview.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.domain.usecase.WordTableConverter;

public class TableConverterController {
    private static final Logger log = LoggerFactory.getLogger(TableConverterController.class);

    @FXML private TextArea inputArea;
    @FXML private TextArea outputArea;

    // Зависимость (можно внедрить через конструктор или создать тут, так как это простая утилита)
    private final WordTableConverter converter = new WordTableConverter();

    @FXML
    private void handleConvert() {
        String rawText = inputArea.getText();
        try {
            String result = converter.convert(rawText);
            outputArea.setText(result.isEmpty() ? "// No valid table data found" : result);
            log.debug("Conversion completed. Input length: {}", rawText.length());
        } catch (Exception e) {
            log.error("Conversion failed", e);
            outputArea.setText("// Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCopy() {
        String text = outputArea.getText();
        if (text != null && !text.isEmpty() && !text.startsWith("//")) {
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
}