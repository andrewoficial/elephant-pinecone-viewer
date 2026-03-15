package ru.kantser.pineview.ui.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kantser.pineview.domain.model.RecordData;
import java.util.Map;

public class RecordViewModel {
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    
    private final String id;
    private final String text;
    private final String metadataJson;
    private final float[] vector;
    private final int dimension;

    public RecordViewModel(RecordData data) {
        this.id = data.id();
        this.text = extractText(data.metadata());
        this.metadataJson = formatMetadata(data.metadata());
        this.vector = data.vector();
        this.dimension = vector != null ? vector.length : 0;
    }

    private String extractText(Map<String, Object> metadata) {
        if (metadata == null) return "";
        Object t = metadata.get("text");
        return t != null ? t.toString() : "";
    }

    private String formatMetadata(Map<String, Object> metadata) {
        if (metadata == null) return "{}";
        try {
            var copy = new java.util.HashMap<>(metadata);
            if (copy.containsKey("text")) {
                String txt = copy.get("text").toString();
                copy.put("text", txt.length() > 50 ? txt.substring(0, 47) + "..." : txt);
            }
            return jsonMapper.writeValueAsString(copy);
        } catch (Exception e) {
            return metadata.toString();
        }
    }

    public String getId() { return id; }
    public String getTextPreview() { return text.length() > 80 ? text.substring(0, 77) + "..." : text; }
    public String getMetadataPreview() {
        String json = metadataJson;
        return json.length() > 40 ? json.substring(0, 37) + "..." : json;
    }
    public String getFullText() { return text; }
    public String getMetadataJson() { return metadataJson; }
    public String getVectorPreview() {
        if (vector == null) return "null";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(10, vector.length); i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.4f", vector[i]));
        }
        if (vector.length > 10) sb.append(", ...");
        sb.append("]");
        return sb.toString();
    }
    public int getVectorDimension() { return dimension; }
}