package ru.kantser.pineview.ui;

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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kantser.pineview.adapter.embedding.MockEmbeddingAdapter;
import ru.kantser.pineview.adapter.pinecone.PineconeApiAdapter;
import ru.kantser.pineview.domain.model.RecordData;
import ru.kantser.pineview.domain.port.RecordPort;
import ru.kantser.pineview.domain.usecase.RecordService;
import ru.kantser.pineview.ui.model.RecordViewModel;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IndexRecordsWindow {
    private static final Logger log = LoggerFactory.getLogger(IndexRecordsWindow.class);

    // UI элементы
    @FXML private Label indexNameLabel;
    @FXML private Label recordCountLabel;
    @FXML private TextField searchField;
    @FXML private TableView<RecordViewModel> recordsTable;
    @FXML private TableColumn<RecordViewModel, String> colId;
    @FXML private TableColumn<RecordViewModel, String> colText;
    @FXML private TableColumn<RecordViewModel, String> colMetadata;
    @FXML private TableColumn<RecordViewModel, Void> colActions;
    @FXML private VBox detailPanel;
    @FXML private VBox placeholderPanel;
    @FXML private Label detailId;
    @FXML private TextArea detailText;
    @FXML private TextArea detailMetadata;
    @FXML private Label vectorSummary;
    @FXML private TextArea detailVector;

    // Новые элементы
    @FXML private Button toggleMetaVectorBtn;
    @FXML private HBox metaVectorBox;

    // Dependencies
    private RecordPort recordPort;
    private RecordService recordService;
    private String indexName;

    // State
    private final ObservableList<RecordViewModel> records = FXCollections.observableArrayList();
    private RecordViewModel selectedRecord;

    public void init(RecordPort recordPort, String indexName, String apiKey) {
        this.recordPort = recordPort;
        this.indexName = indexName;
        this.recordService = new RecordService(recordPort, new MockEmbeddingAdapter());

        indexNameLabel.setText("📦 " + indexName);
        setupTable();
        loadRecords();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colText.setCellValueFactory(new PropertyValueFactory<>("textPreview"));
        colMetadata.setCellValueFactory(new PropertyValueFactory<>("metadataPreview"));

        // === Настройка ширин ===
        colId.setPrefWidth(100);
        colText.setPrefWidth(400);
        colMetadata.setPrefWidth(200);
        colActions.setPrefWidth(80);

        // === Ограничение высоты строки (примерно 2-3 строки) ===
        colText.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.maxWidthProperty().bind(col.widthProperty().subtract(10));
                // Макс высота ~45px (около 2-3 строк)
                label.setMaxHeight(45);
                label.setStyle("-fx-font-size: 11px; -fx-text-fill: #cdd6f4;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    setGraphic(label);
                }
            }
        });

        colMetadata.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.maxWidthProperty().bind(col.widthProperty().subtract(10));
                label.setMaxHeight(45);
                label.setStyle("-fx-font-size: 11px; -fx-text-fill: #a6adc8;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    label.setText(item);
                    setGraphic(label);
                }
            }
        });

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
                if (empty) setGraphic(null);
                else setGraphic(new HBox(4, editBtn, deleteBtn));
            }
        });

        recordsTable.setItems(records);
        recordsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                selectedRecord = selected;
                showDetails(selected);
            }
        });
    }

    private void loadRecords() {
        log.info("Loading records for index: {}", indexName);
        recordCountLabel.setText("(loading...)");
        recordPort.fetchAllRecords(indexName)
                .thenAccept(this::handleLoadSuccess)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        recordCountLabel.setText("(error)");
                        showAlert("Error", "Failed to load records: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void handleLoadSuccess(List<RecordData> recordList) {
        Platform.runLater(() -> {
            if (recordList == null || recordList.isEmpty()) {
                recordCountLabel.setText("(0 records)");
                records.clear();
            } else {
                var displayList = recordList.stream()
                        .map(RecordViewModel::new)
                        .collect(Collectors.toCollection(FXCollections::observableArrayList));
                records.setAll(displayList);
                recordCountLabel.setText("(" + displayList.size() + " records)");
            }
        });
    }

    private void showDetails(RecordViewModel record) {
        placeholderPanel.setVisible(false);
        placeholderPanel.setManaged(false);
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);

        detailId.setText(record.getId());
        detailText.setText(record.getFullText());
        detailMetadata.setText(record.getMetadataJson());
        vectorSummary.setText("Vector (" + record.getVectorDimension() + "D)");
        detailVector.setText(record.getVectorPreview());

        // Скрываем блок мета/вектор при выборе новой записи
        metaVectorBox.setVisible(false);
        metaVectorBox.setManaged(false);
        toggleMetaVectorBtn.setText("▼ Show Meta & Vector");
    }

    @FXML
    private void toggleMetaVector() {
        boolean isVisible = metaVectorBox.isVisible();
        metaVectorBox.setVisible(!isVisible);
        metaVectorBox.setManaged(!isVisible);
        toggleMetaVectorBtn.setText(isVisible ? "▼ Show Meta & Vector" : "▲ Hide Meta & Vector");
    }

    @FXML
    private void copyId() {
        if (selectedRecord != null) {
            final var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            final var content = new javafx.scene.input.ClipboardContent();
            content.putString(selectedRecord.getId());
            clipboard.setContent(content);
        }
    }

    @FXML
    private void handleRefresh() { loadRecords(); }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) {
            recordsTable.setItems(records);
            recordCountLabel.setText("(" + records.size() + " records)");
            return;
        }
        ObservableList<RecordViewModel> filtered = records.stream()
                .filter(r -> r.getId().toLowerCase().contains(query) ||
                        r.getFullText().toLowerCase().contains(query) ||
                        r.getMetadataJson().toLowerCase().contains(query))
                .collect(FXCollections::observableArrayList, ObservableList::add, ObservableList::addAll);
        recordsTable.setItems(filtered);
        recordCountLabel.setText("(" + filtered.size() + " filtered)");
    }

    @FXML private void handleAddRecord() { openRecordEditor(null); }
    @FXML private void handleEditSelected() { if (selectedRecord != null) openRecordEditor(selectedRecord); }
    @FXML private void handleDeleteSelected() { if (selectedRecord != null) handleDelete(selectedRecord); }
    private void handleEdit(RecordViewModel record) { openRecordEditor(record); }

    private void handleDelete(RecordViewModel record) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete record \"" + record.getId() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        if (confirm.showAndWait().filter(ButtonType.YES::equals).isPresent()) {
            recordPort.deleteRecord(indexName, record.getId())
                    .thenRun(() -> Platform.runLater(() -> {
                        records.remove(record);
                        recordCountLabel.setText("(" + records.size() + " records)");
                        if (selectedRecord == record) {
                            placeholderPanel.setVisible(true);
                            placeholderPanel.setManaged(true);
                            detailPanel.setVisible(false);
                            detailPanel.setManaged(false);
                            selectedRecord = null;
                        }
                        showAlert("Deleted", "Record deleted");
                    }))
                    .exceptionally(ex -> { Platform.runLater(() -> showAlert("Error", ex.getMessage())); return null; });
        }
    }

    private void openRecordEditor(RecordViewModel existing) {
        // (Код диалога редактирования остается без изменений)
        try {
            Dialog<RecordEditResult> dialog = new Dialog<>();
            dialog.setTitle(existing == null ? "➕ Add Record" : "✏️ Edit Record");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));
            TextField idField = new TextField(existing != null ? existing.getId() : "");
            idField.setPromptText("Record ID");
            if (existing != null) idField.setDisable(true);
            TextArea textField = new TextArea(existing != null ? existing.getFullText() : "");
            textField.setPromptText("Text content");
            textField.setPrefHeight(100);

            grid.add(new Label("ID:"), 0, 0); grid.add(idField, 1, 0);
            grid.add(new Label("Text:"), 0, 1); grid.add(textField, 1, 1);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/ru/kantser/pineview/styles.css").toExternalForm());

            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK && !idField.getText().trim().isEmpty() && !textField.getText().trim().isEmpty()) {
                    return new RecordEditResult(idField.getText().trim(), textField.getText());
                }
                return null;
            });

            dialog.showAndWait().ifPresent(result -> {
                recordService.saveRecord(indexName, result.id, result.text, Map.of())
                        .thenRun(() -> Platform.runLater(() -> { loadRecords(); showAlert("Success", "Record saved"); }))
                        .exceptionally(ex -> { Platform.runLater(() -> showAlert("Error", ex.getMessage())); return null; });
            });
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML private void handleImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Lines", "*.jsonl"));
        File file = fileChooser.showOpenDialog(recordsTable.getScene().getWindow());
        if (file != null) {
            recordService.importFromFile(indexName, file)
                    .thenAccept(result -> Platform.runLater(() -> { loadRecords(); showAlert("Import", "Imported " + result.successCount); }))
                    .exceptionally(ex -> { Platform.runLater(() -> showAlert("Error", ex.getMessage())); return null; });
        }
    }

    @FXML private void handleClose() { ((Stage) recordsTable.getScene().getWindow()).close(); }
    private void showAlert(String title, String message) { new Alert(Alert.AlertType.INFORMATION, message).showAndWait(); }

    private record RecordEditResult(String id, String text) {}

}