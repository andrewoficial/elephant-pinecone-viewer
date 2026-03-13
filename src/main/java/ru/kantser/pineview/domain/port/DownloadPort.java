package ru.kantser.pineview.domain.port;

import java.io.IOException;

public interface DownloadPort {
    void download(String url, String destinationPath, ProgressCallback callback) throws IOException;
    
    interface ProgressCallback {
        void onProgress(long bytesRead, long totalBytes);
    }
}