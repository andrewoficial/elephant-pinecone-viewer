package ru.kantser.pineview.adapter.download;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.domain.port.DownloadPort;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class HttpDownloadAdapter implements DownloadPort {
    private static final Logger log = LoggerFactory.getLogger(HttpDownloadAdapter.class);
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void download(String url, String destinationPath, ProgressCallback callback) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download: HTTP " + response.code());
            }

            long contentLength = response.body().contentLength();
            try (InputStream is = response.body().byteStream();
                 FileOutputStream fos = new FileOutputStream(new File(destinationPath))) {

                byte[] buffer = new byte[8192];
                long bytesRead = 0;
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    bytesRead += len;
                    if (callback != null) {
                        callback.onProgress(bytesRead, contentLength);
                    }
                }
                log.info("Downloaded {} bytes to {}", bytesRead, destinationPath);
            }
        }
    }
}