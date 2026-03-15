package ru.kantser.pineview.adapter.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.domain.model.ReleaseInfo;
import ru.kantser.pineview.domain.port.ReleaseInfoPort;
import java.io.IOException;

public class GitHubReleaseAdapter implements ReleaseInfoPort {
    private static final Logger log = LoggerFactory.getLogger(GitHubReleaseAdapter.class);

    private static final String REPO_API = "https://api.github.com/repos/andrewoficial/elephant-pinecone-viewer/releases/latest";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public ReleaseInfo fetchLatestRelease() throws IOException {
        Request request = new Request.Builder()
                .url(REPO_API)
                .header("Accept", "application/vnd.github.v3+json") // Хороший тон для GitHub API
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("GitHub API error: " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "{}";
            JsonNode root = mapper.readTree(responseBody);

            String tag = root.path("tag_name").asText();
            String version = tag.startsWith("v") ? tag.substring(1) : tag;

            String notes = root.path("body").asText();
            String downloadUrl = extractDownloadUrl(root);

            return new ReleaseInfo(version, notes, downloadUrl);
        }
    }

    private String extractDownloadUrl(JsonNode root) {
        JsonNode assets = root.path("assets"); // Используем path для безопасности от NPE

        if (assets.isMissingNode() || !assets.isArray()) {
            return null;
        }

        for (JsonNode asset : assets) {
            String name = asset.path("name").asText();

            if (name.startsWith("Elephant-Pinecone-Viewer") && name.endsWith(".jar")) {
                return asset.path("browser_download_url").asText();
            }
        }

        return null;
    }
}