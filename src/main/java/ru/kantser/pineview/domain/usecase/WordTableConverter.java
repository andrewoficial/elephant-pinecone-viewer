package ru.kantser.pineview.domain.usecase;

import java.util.Arrays;
import java.util.stream.Collectors;

public class WordTableConverter {

    public String convert(String rawText) {
        if (rawText == null || rawText.isBlank()) return "";

        String[] lines = rawText.split("\\n");
        StringBuilder md = new StringBuilder();
        int columnCount = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // Разделяем по табуляции
            String[] cells = line.split("\\t");

            // Определяем количество колонок по первой не пустой строке
            if (columnCount == 0) {
                columnCount = cells.length;
            }

            // Формируем строку Markdown
            String mdRow = Arrays.stream(cells)
                    .map(cell -> " " + cell.trim() + " ")
                    .collect(Collectors.joining("|", "|", "|"));

            md.append(mdRow).append("\n");

            // После первой строки добавляем разделитель заголовка
            if (i == 0) {
                md.append(generateSeparator(columnCount)).append("\n");
            }
        }

        return md.toString();
    }

    private String generateSeparator(int count) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < count; i++) {
            sb.append(" --- |");
        }
        return sb.toString();
    }
}