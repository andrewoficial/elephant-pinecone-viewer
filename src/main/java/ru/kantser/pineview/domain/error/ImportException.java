package ru.kantser.pineview.domain.error;

public class ImportException extends RuntimeException {
    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}