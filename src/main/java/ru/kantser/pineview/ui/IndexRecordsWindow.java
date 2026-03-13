package ru.kantser.pineview.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.adapter.pinecone.PineconeApiAdapter;
import ru.kantser.pineview.domain.model.RecordData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class IndexRecordsWindow {
    private static final Logger log = LoggerFactory.getLogger(IndexRecordsWindow.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    // UI элементы
    @FXML private Label indexNameLabel;
    @FXML private Label recordCountLabel;
    @FXML private TextField searchField;
    @FXML private TableView<RecordDisplay> recordsTable;
    @FXML private TableColumn<RecordDisplay, String> colId;
    @FXML private TableColumn<RecordDisplay, String> colText;
    @FXML private TableColumn<RecordDisplay, String> colMetadata;
    @FXML private TableColumn<RecordDisplay, Void> colActions;
    @FXML private VBox detailPanel;
    @FXML private VBox placeholderPanel;
    @FXML private Label detailId;
    @FXML private TextArea detailText;
    @FXML private TextArea detailMetadata;
    @FXML private Label vectorSummary;
    @FXML private Button toggleVectorBtn;
    @FXML private TextArea detailVector;

    private PineconeApiAdapter pineconeAdapter;
    private String indexName;
    private String apiKey;
    private final ObservableList<RecordDisplay> records = FXCollections.observableArrayList();
    private RecordDisplay selectedRecord;

    public void init(PineconeApiAdapter adapter, String indexName, String apiKey) {
        this.pineconeAdapter = adapter;
        this.indexName = indexName;
        this.apiKey = apiKey;

        indexNameLabel.setText("📦 " + indexName);
        setupTable();
        loadRecords();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colText.setCellValueFactory(new PropertyValueFactory<>("textPreview"));
        colMetadata.setCellValueFactory(new PropertyValueFactory<>("metadataPreview"));

        // === Фиксация ширины колонок ===
        // Это предотвращает "прыжки" и некрасивое изменение ширины таблицы
        fixColumnWidth(colId, 250);
        fixColumnWidth(colText, 500);
        fixColumnWidth(colMetadata, 250);
        fixColumnWidth(colActions, 100);

        // Кнопки действий в каждой строке
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");

            {
                editBtn.setStyle("-fx-padding: 2 6; -fx-font-size: 11px;");
                editBtn.setOnAction(e -> handleEdit(getTableView().getItems().get(getIndex())));

                deleteBtn.setStyle("-fx-padding: 2 6; -fx-font-size: 11px; -fx-text-fill: #f38ba8;");
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(4, editBtn, deleteBtn));
                }
            }
        });

        recordsTable.setItems(records);

        recordsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> {
                    if (selected != null) {
                        selectedRecord = selected;
                        showDetails(selected);
                    }
                }
        );
    }

    // Вспомогательный метод для фиксации ширины
    private void fixColumnWidth(TableColumn<?, ?> col, double width) {
        col.setPrefWidth(width);
        col.setMinWidth(width);
        col.setMaxWidth(width);
        col.setResizable(false);
    }

    private void loadRecords() {
        log.info("Loading records for index: {}", indexName);
        recordCountLabel.setText("(loading...)");

        pineconeAdapter.fetchAllRecords(indexName)
                .thenAccept(recordList -> Platform.runLater(() -> {
                    if (recordList == null || recordList.isEmpty()) {
                        recordCountLabel.setText("(0 records)");
                        records.clear();
                    } else {
                        var displayList = recordList.stream()
                                .map(RecordDisplay::new)
                                .collect(Collectors.toCollection(FXCollections::observableArrayList));

                        records.setAll(displayList);
                        recordCountLabel.setText("(" + displayList.size() + " records)");
                        log.info("Loaded {} records", displayList.size());
                    }
                }))
                .exceptionally(ex -> {
                    log.error("Failed to load records", ex);
                    Platform.runLater(() -> {
                        recordCountLabel.setText("(error)");
                        showAlert("Error", "Failed to load records: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void showDetails(RecordDisplay record) {
        placeholderPanel.setVisible(false);
        placeholderPanel.setManaged(false);
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
        detailId.setText(record.getId());
        detailText.setText(record.getFullText());
        detailMetadata.setText(record.getMetadataJson());

        int dim = record.getVectorDimension();
        vectorSummary.setText(dim + "-dimensional vector");
        detailVector.setText(record.getVectorPreview());

        toggleVectorBtn.setText("▼ Show vector");
        detailVector.setVisible(false);
    }

    @FXML
    private void toggleVector() {
        boolean visible = !detailVector.isVisible();
        detailVector.setVisible(visible);
        toggleVectorBtn.setText(visible ? "▲ Hide vector" : "▼ Show vector");
    }

    @FXML
    private void copyId() {
        if (selectedRecord != null) {
            final var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            final var content = new javafx.scene.input.ClipboardContent();
            content.putString(selectedRecord.getId());
            clipboard.setContent(content);
            log.info("Copied ID to clipboard: {}", selectedRecord.getId());
        }
    }

    @FXML
    private void handleRefresh() {
        loadRecords();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) {
            recordsTable.setItems(records);
            return;
        }

        ObservableList<RecordDisplay> filtered = records.stream()
                .filter(r -> r.getId().toLowerCase().contains(query) ||
                        r.getFullText().toLowerCase().contains(query) ||
                        r.getMetadataJson().toLowerCase().contains(query))
                .collect(FXCollections::observableArrayList,
                        ObservableList::add,
                        ObservableList::addAll);

        recordsTable.setItems(filtered);
        recordCountLabel.setText("(" + filtered.size() + " filtered)");
    }

    @FXML
    private void handleAddRecord() {
        openRecordEditor(null);
    }

    @FXML
    private void handleEditSelected() {
        if (selectedRecord != null) {
            openRecordEditor(selectedRecord);
        }
    }

    @FXML
    private void handleDeleteSelected() {
        if (selectedRecord != null) {
            handleDelete(selectedRecord);
        }
    }

    private void handleEdit(RecordDisplay record) {
        openRecordEditor(record);
    }

    private void handleDelete(RecordDisplay record) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete record \"" + record.getId() + "\" from index \"" + indexName + "\"?\n\nThis action cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");

        if (confirm.showAndWait().filter(ButtonType.YES::equals).isPresent()) {
            pineconeAdapter.deleteRecord(indexName, record.getId())
                    .thenRun(() -> Platform.runLater(() -> {
                        log.info("Deleted record: {}", record.getId());
                        records.remove(record);
                        recordCountLabel.setText("(" + records.size() + " records)");
                        if (selectedRecord == record) {
                            placeholderPanel.setVisible(true);
                            placeholderPanel.setManaged(true);
                            detailPanel.setVisible(false);
                            detailPanel.setManaged(false);
                            selectedRecord = null;
                        }
                        showAlert("Deleted", "Record deleted successfully");
                    }))
                    .exceptionally(ex -> {
                        log.error("Delete failed", ex);
                        Platform.runLater(() ->
                                showAlert("Error", "Failed to delete: " + ex.getMessage()));
                        return null;
                    });
        }
    }

    private void openRecordEditor(RecordDisplay existing) {
        try {
            Dialog<RecordEditResult> dialog = new Dialog<>();
            dialog.setTitle(existing == null ? "➕ Add Record" : "✏️ Edit Record");
            dialog.setHeaderText("Index: " + indexName);

            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField idField = new TextField(existing != null ? existing.getId() : "");
            idField.setPromptText("Record ID (unique)");
            idField.setPrefWidth(300);

            TextArea textField = new TextArea(existing != null ? existing.getFullText() : "");
            textField.setPromptText("Text content (vector will be generated automatically)");
            textField.setPrefHeight(200);
            textField.setPrefWidth(400);
            textField.setWrapText(true);

            grid.add(new Label("ID:"), 0, 0);
            grid.add(idField, 1, 0);
            grid.add(new Label("Text:"), 0, 1);
            grid.add(textField, 1, 1);

            Label hint = new Label("💡 Vector will be generated automatically from your text");
            hint.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");
            grid.add(hint, 1, 2);

            dialog.getDialogPane().setContent(grid);

            dialog.getDialogPane().getStylesheets().add(
                    getClass().getResource("/ru/kantser/pineview/styles.css").toExternalForm()
            );

            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK) {
                    String id = idField.getText().trim();
                    String text = textField.getText();
                    if (id.isEmpty()) {
                        showAlert("Error", "ID cannot be empty");
                        return null;
                    }
                    if (text == null || text.trim().isEmpty()) {
                        showAlert("Error", "Text cannot be empty");
                        return null;
                    }
                    return new RecordEditResult(id, text, null, null);
                }
                return null;
            });

            dialog.showAndWait().ifPresent(result -> {
                if (result != null && result.isValid()) {
                    saveRecordSimple(result, existing);
                }
            });

        } catch (Exception e) {
            log.error("Failed to open editor", e);
            showAlert("Error", "Could not open editor: " + e.getMessage());
        }
    }

    private void saveRecordSimple(RecordEditResult result, RecordDisplay existing) {
        String id = result.id;
        String text = result.text;

        float[] vector = generateEmbeddingForText(text);

        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("text", text);
        metadata.put("source", "pineview-client");
        metadata.put("timestamp", System.currentTimeMillis());

        pineconeAdapter.upsertRecord(indexName, id, vector, metadata)
                .thenRun(() -> Platform.runLater(() -> {
                    log.info("Upserted record: {}", id);
                    loadRecords();
                    showAlert("Success", "Record " + (existing != null ? "updated" : "added") + " successfully");
                }))
                .exceptionally(ex -> {
                    log.error("Upsert failed", ex);
                    Platform.runLater(() ->
                            showAlert("Error", "Failed to save: " + ex.getMessage()));
                    return null;
                });
    }

    private float[] generateEmbeddingForText(String text) {
        int dimension = 2048;
        int hash = text.hashCode();
        float[] vector = new float[dimension];
        java.util.Random rng = new java.util.Random(hash);
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) (rng.nextGaussian() * 0.01);
        }
        return vector;
    }

    private void saveRecord(RecordEditResult result, RecordDisplay existing) {
        String id = result.id;
        if (id.isEmpty()) {
            showAlert("Error", "ID cannot be empty");
            return;
        }

        Map<String, Object> metadata;
        try {
            metadata = jsonMapper.readValue(result.metadataJson, Map.class);
        } catch (Exception e) {
            showAlert("Error", "Invalid metadata JSON: " + e.getMessage());
            return;
        }

        if (result.text != null && !result.text.isEmpty()) {
            metadata.put("text", result.text);
        }

        float[] vector;
        try {
            vector = jsonMapper.readValue(result.vectorJson, float[].class);
        } catch (Exception e) {
            showAlert("Error", "Invalid vector JSON: " + e.getMessage());
            return;
        }

        pineconeAdapter.upsertRecord(indexName, id, vector, metadata)
                .thenRun(() -> Platform.runLater(() -> {
                    log.info("Upserted record: {}", id);
                    loadRecords();
                    showAlert("Success", "Record " + (existing != null ? "updated" : "added") + " successfully");
                }))
                .exceptionally(ex -> {
                    log.error("Upsert failed", ex);
                    Platform.runLater(() ->
                            showAlert("Error", "Failed to save: " + ex.getMessage()));
                    return null;
                });
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) recordsTable.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.showAndWait();
    }

    public static class RecordDisplay {
        private final String id;
        private final String text;
        private final String metadataJson;
        private final float[] vector;
        private final int dimension;

        public RecordDisplay(RecordData data) {
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
        public String getTextPreview() {
            return text.length() > 80 ? text.substring(0, 77) + "..." : text;
        }
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

    public static class RecordEditResult {
        final String id, text, metadataJson, vectorJson;

        public RecordEditResult(String id, String text, String metadataJson, String vectorJson) {
            this.id = id;
            this.text = text;
            this.metadataJson = metadataJson;
            this.vectorJson = vectorJson;
        }

        public boolean isValid() {
            return id != null && !id.isEmpty();
        }
    }
}