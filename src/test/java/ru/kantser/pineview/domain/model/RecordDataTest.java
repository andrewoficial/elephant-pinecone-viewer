package ru.kantser.pineview.domain.model;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecordDataTest {

    @Test
    void testRecordCreationAndGetters() {
        // Подготовка данных
        String id = "rec-123";
        float[] vector = {0.1f, 0.2f, 0.3f};
        Map<String, Object> metadata = Map.of("author", "test", "year", 2023);

        // Создание объекта
        RecordData record = new RecordData(id, vector, metadata);

        // Проверки (Assertions)
        assertEquals(id, record.id(), "ID должен совпадать");
        assertArrayEquals(vector, record.vector(), "Вектор должен совпадать");
        assertEquals(metadata, record.metadata(), "Метаданные должны совпадать");
        assertEquals("test", record.metadata().get("author"));
    }

    @Test
    void testNullValues() {
        // Проверка на null (record позволяет null)
        RecordData record = new RecordData("id-null", null, null);
        
        assertEquals("id-null", record.id());
        assertNull(record.vector());
        assertNull(record.metadata());
    }
}