package ru.kantser.pineview.domain.model;

public class ReleaseInfo {
    private final String version;
    private final String releaseNotes;
    private final String downloadUrl;

    public ReleaseInfo(String version, String releaseNotes, String downloadUrl) {
        this.version = version;
        this.releaseNotes = releaseNotes;
        this.downloadUrl = downloadUrl;
    }

    public String getVersion() { return version; }
    public String getReleaseNotes() { return releaseNotes; }
    public String getDownloadUrl() { return downloadUrl; }
}