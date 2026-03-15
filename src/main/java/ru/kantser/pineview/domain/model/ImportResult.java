package ru.kantser.pineview.domain.model;

public record ImportResult(int totalParsed, int successfullySaved, String sourceFile) {
    public int getFailedCount() {
        return totalParsed - successfullySaved;
    }
}