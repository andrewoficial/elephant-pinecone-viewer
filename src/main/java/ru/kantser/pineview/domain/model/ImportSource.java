package ru.kantser.pineview.domain.model;

import java.io.InputStream;

public class ImportSource {
    private final InputStream inputStream;
    private final String fileName;
    private final String encoding;
    
    public ImportSource(InputStream inputStream, String fileName, String encoding) {
        this.inputStream = inputStream;
        this.fileName = fileName;
        this.encoding = encoding;
    }
    
    public InputStream getStream() { return inputStream; }
    public String getFileName() { return fileName; }
    public String getEncoding() { return encoding; }
    
    public String getExtension() {
        int dot = fileName.lastIndexOf('.');
        return (dot == -1) ? "" : fileName.substring(dot + 1).toLowerCase();
    }
}