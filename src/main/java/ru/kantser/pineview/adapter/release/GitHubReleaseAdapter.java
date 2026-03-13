package ru.kantser.pineview.adapter.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ru.kantser.pineview.domain.model.ReleaseInfo;
import ru.kantser.pineview.domain.port.ReleaseInfoPort;
import java.io.IOException;

public class GitHubReleaseAdapter implements ReleaseInfoPort {
    private static final String REPO_API = "https://api.github.com/repos/ваш-username/PineView/releases/latest";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public ReleaseInfo fetchLatestRelease() throws IOException {
        Request request = new Request.Builder().url(REPO_API).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("GitHub API error: " + response.code());
            }
            JsonNode root = mapper.readTree(response.body().string());
            String version = root.get("tag_name").asText();
            String notes = root.get("body").asText();
            String downloadUrl = extractDownloadUrl(root);
            return new ReleaseInfo(version, notes, downloadUrl);
        }
    }

    private String extractDownloadUrl(JsonNode root) {
        JsonNode assets = root.get("assets");
        for (JsonNode asset : assets) {
            String name = asset.get("name").asText();
            if (name.endsWith(".jar")) {
                return asset.get("browser_download_url").asText();
            }
        }
        return null; // или выбросить исключение
    }
}