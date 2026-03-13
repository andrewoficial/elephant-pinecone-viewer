package ru.kantser.pineview.adapter.pinecone;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.pinecone.clients.Index;
import io.pinecone.clients.Pinecone;
import io.pinecone.proto.*;
import io.pinecone.unsigned_indices_model.QueryResponseWithUnsignedIndices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.db_control.client.model.IndexList;
import org.openapitools.db_control.client.model.IndexModel;
import ru.kantser.pineview.domain.model.HealthReport;
import ru.kantser.pineview.domain.model.RecordData;
import ru.kantser.pineview.domain.model.ServiceStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PineconeApiAdapterTest {

    @Mock
    private Pinecone mockPinecone;

    @Mock
    private Index mockIndex;

    private PineconeApiAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PineconeApiAdapter();
        // Внедряем мок через тестовый метод
        adapter.setPineconeClient(mockPinecone);
    }

    // === ТЕСТЫ HEALTH CHECK ===

    @Test
    void checkHealth_ShouldReturnOnline_WhenListIndexesSucceeds() throws Exception {
        // Arrange
        IndexList fakeIndexList = new IndexList();
        fakeIndexList.setIndexes(List.of(new IndexModel())); // 1 индекс
        when(mockPinecone.listIndexes()).thenReturn(fakeIndexList);

        // Act
        CompletableFuture<HealthReport> future = adapter.checkHealth("Pinecone", "url");
        HealthReport report = future.get();

        // Assert
        assertEquals(ServiceStatus.ONLINE, report.getStatus());
        assertTrue(report.getMessage().contains("1 indexes"));
        verify(mockPinecone).listIndexes();
    }

    @Test
    void checkHealth_ShouldReturnOffline_WhenApiThrowsException() throws Exception {
        // Arrange
        when(mockPinecone.listIndexes()).thenThrow(new RuntimeException("Network error"));

        // Act
        HealthReport report = adapter.checkHealth("Pinecone", "url").get();

        // Assert
        assertEquals(ServiceStatus.OFFLINE, report.getStatus());
        assertTrue(report.getMessage().contains("Network error"));
    }

    // === ТЕСТЫ FETCH RECORDS ===

    @Test
    void fetchAllRecords_ShouldReturnRecords_WhenQuerySucceeds() {
        // Arrange
        when(mockPinecone.getIndexConnection("test-index")).thenReturn(mockIndex);

        // 1. Мокаем describeIndexStats (нужна размерность)
        DescribeIndexStatsResponse statsResponse = DescribeIndexStatsResponse.newBuilder()
                .setDimension(2)
                .build();
        when(mockIndex.describeIndexStats()).thenReturn(statsResponse);

        // 2. Готовим Protobuf ответ
        // Создаем метаданные: {"author": "test"}
        Struct metadata = Struct.newBuilder()
                .putFields("author", Value.newBuilder().setStringValue("test").build())
                .build();

        // ИСПРАВЛЕНО: Используем ScoredVector вместо Vector, так как QueryResponse содержит список ScoredVector
        ScoredVector scoredVector = ScoredVector.newBuilder()
                .setId("rec-1")
                .addValues(0.5f)
                .addValues(0.6f)
                .setMetadata(metadata)
                .setScore(0.99f) // Score обязателен для ScoredVector
                .build();


        // ИСПРАВЛЕНО: Используем QueryResponseWithUnsignedIndices, так как его возвращает mockIndex.query()
        io.pinecone.proto.QueryResponse protoResponse = io.pinecone.proto.QueryResponse.newBuilder()
                .addMatches(scoredVector) // Вкладываем созданный вектор
                .build();

        QueryResponseWithUnsignedIndices queryResponse = new QueryResponseWithUnsignedIndices(protoResponse);

        // Stubbing возвращает исправленный тип
        when(mockIndex.query(anyInt(), anyList(), any(), any(), any(), anyString(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(queryResponse);

        // Act
        List<RecordData> records = adapter.fetchAllRecords("test-index").join();

        // Assert
        assertNotNull(records);
        assertEquals(1, records.size());

        RecordData data = records.get(0);
        assertEquals("rec-1", data.id());
        assertArrayEquals(new float[]{0.5f, 0.6f}, data.vector());
        assertEquals("test", data.metadata().get("author"));
    }

    // === ТЕСТЫ UPSERT ===

    @Test
    void upsertRecord_ShouldCallUpsertWithCorrectParams() {
        // Arrange
        when(mockPinecone.getIndexConnection("test-index")).thenReturn(mockIndex);

        // ИСПРАВЛЕНО: upsert возвращает UpsertResponse, поэтому используем when().thenReturn()
        // doNothing() годится только для void методов.
        when(mockIndex.upsert(anyString(), anyList(), any(), any(), any(Struct.class), anyString()))
                .thenReturn(io.pinecone.proto.UpsertResponse.getDefaultInstance());

        Map<String, Object> metadata = Map.of("key", "value");
        float[] vector = {0.1f, 0.2f};

        // Act
        adapter.upsertRecord("test-index", "id-1", vector, metadata).join();

        // Assert
        verify(mockIndex).upsert(
                eq("id-1"),
                anyList(),
                isNull(),
                isNull(),
                any(Struct.class),
                eq("")
        );
    }

    // === ТЕСТЫ DELETE ===

    @Test
    void deleteRecord_ShouldCallDeleteOnIndex() {
        // Arrange
        when(mockPinecone.getIndexConnection("test-index")).thenReturn(mockIndex);

        // ИСПРАВЛЕНО: delete возвращает DeleteResponse
        when(mockIndex.delete(anyList(), anyBoolean(), anyString(), any()))
                .thenReturn(io.pinecone.proto.DeleteResponse.getDefaultInstance());

        // Act
        adapter.deleteRecord("test-index", "id-to-delete").join();

        // Assert
        verify(mockIndex).delete(
                eq(List.of("id-to-delete")),
                eq(false),
                eq("__default__"),
                isNull()
        );
    }
}