package ru.kantser.pineview.domain.usecase;

import ru.kantser.pineview.domain.model.ReleaseInfo;
import ru.kantser.pineview.domain.port.ReleaseInfoPort;
import ru.kantser.pineview.domain.port.VersionPort;
import ru.kantser.pineview.domain.port.DownloadPort; // если используем
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class CheckForUpdatesUseCase {
    private static final Logger log = LoggerFactory.getLogger(CheckForUpdatesUseCase.class);

    private final VersionPort versionPort;
    private final ReleaseInfoPort releaseInfoPort;
    private final DownloadPort downloadPort; // опционально

    private ReleaseInfo latestRelease; // кеш

    public CheckForUpdatesUseCase(VersionPort versionPort, ReleaseInfoPort releaseInfoPort, DownloadPort downloadPort) {
        this.versionPort = versionPort;
        this.releaseInfoPort = releaseInfoPort;
        this.downloadPort = downloadPort;
    }

    public boolean isUpdateAvailable() throws IOException {
        if (latestRelease == null) {
            latestRelease = releaseInfoPort.fetchLatestRelease();
        }
        String current = versionPort.getCurrentVersion();
        return !latestRelease.getVersion().equals(current);
    }

    public ReleaseInfo getLatestRelease() throws IOException {
        if (latestRelease == null) {
            latestRelease = releaseInfoPort.fetchLatestRelease();
        }
        return latestRelease;
    }

    public void downloadUpdate(DownloadPort.ProgressCallback callback) throws IOException {
        if (latestRelease == null || latestRelease.getDownloadUrl() == null) {
            throw new IllegalStateException("No release info or download URL");
        }
        // Если используем порт скачивания
        downloadPort.download(latestRelease.getDownloadUrl(), "Elephant-Pinecone-Viewer-"+latestRelease.getVersion()+".jar", callback);
        
        // Или можно оставить прямую реализацию здесь, но тогда usecase будет зависеть от OkHttp.
        // Лучше вынести в адаптер.
    }
}