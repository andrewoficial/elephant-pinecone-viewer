package ru.kantser.pineview.domain.usecase;

import ru.kantser.pineview.domain.model.ReleaseInfo;
import ru.kantser.pineview.domain.port.ReleaseInfoPort;
import ru.kantser.pineview.domain.port.VersionPort;
import ru.kantser.pineview.domain.port.DownloadPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class CheckForUpdatesUseCase {
    private static final Logger log = LoggerFactory.getLogger(CheckForUpdatesUseCase.class);

    // Dependencies
    private final VersionPort versionPort;
    private final ReleaseInfoPort releaseInfoPort;
    private final DownloadPort downloadPort;

    private ReleaseInfo latestRelease;

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
        downloadPort.download(latestRelease.getDownloadUrl(), "Elephant-Pinecone-Viewer-"+latestRelease.getVersion()+".jar", callback);
    }
}