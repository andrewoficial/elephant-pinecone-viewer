package ru.kantser.pineview.adapter.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonConfigAdapterTest {

    // JUnit 5 создаст временную директорию и передаст её сюда
    @TempDir
    Path tempDir;

    private File tempConfigFile;
    private JsonConfigAdapter adapter;

    @BeforeEach
    void setUp() {
        // Создаем ссылку на файл внутри временной папки
        tempConfigFile = tempDir.resolve("test-config.json").toFile();
        
        // Инициализируем адаптер с этим временным файлом
        adapter = new JsonConfigAdapter(tempConfigFile);
    }

    @Test
    void load_shouldReturnEmpty_WhenFileDoesNotExist() {
        // Файл не создан, адаптер должен вернуть Optional.empty
        Optional<String> value = adapter.load("non-existent-key");
        
        assertTrue(value.isEmpty(), "Должен быть пустой Optional, если файла нет");
    }

    @Test
    void save_shouldCreateFile_and_SaveValue() {
        // Act: сохраняем значение
        adapter.save("apiKey", "12345-secret");

        // Assert 1: Проверяем, что файл создался
        assertTrue(tempConfigFile.exists(), "Файл конфигурации должен быть создан");

        // Assert 2: Проверяем, что значение можно прочитать
        Optional<String> loaded = adapter.load("apiKey");
        assertTrue(loaded.isPresent());
        assertEquals("12345-secret", loaded.get());
    }

    @Test
    void loadAll_shouldReturnEmptyMap_WhenFileIsEmpty() throws IOException {
        // Создаем пустой файл
        tempConfigFile.createNewFile();

        Map<String, String> all = adapter.loadAll();

        assertTrue(all.isEmpty(), "Карта должна быть пустой для пустого файла");
    }

    @Test
    void loadAll_shouldReadExistingData() throws IOException {
        // Arrange: Запишем валидный JSON вручную через Files
        String jsonContent = "{\"host\":\"localhost\", \"port\":\"8080\"}";
        Files.writeString(tempConfigFile.toPath(), jsonContent);

        // Act
        Map<String, String> config = adapter.loadAll();

        // Assert
        assertEquals(2, config.size());
        assertEquals("localhost", config.get("host"));
        assertEquals("8080", config.get("port"));
    }

    @Test
    void save_shouldOverwriteExistingValue() {
        // Arrange
        adapter.save("setting", "value1");
        
        // Act
        adapter.save("setting", "value2"); // Перезаписываем

        // Assert
        assertEquals("value2", adapter.load("setting").get());
    }
    
    @Test
    void save_shouldPreserveOtherKeys() {
        // Arrange
        adapter.save("key1", "val1");
        adapter.save("key2", "val2");

        // Act - обновляем первый ключ
        adapter.save("key1", "updated_val1");

        // Assert - второй ключ должен остаться
        assertEquals("updated_val1", adapter.load("key1").get());
        assertEquals("val2", adapter.load("key2").get());
    }

    @Test
    void loadAll_shouldReturnEmptyMap_OnInvalidJson() throws IOException {
        // Arrange: Создаем файл с битым JSON
        Files.writeString(tempConfigFile.toPath(), "this is not { valid json }");

        // Act
        Map<String, String> result = adapter.loadAll();

        // Assert: В реализации адаптера catch(IOException) возвращает new HashMap<>()
        assertNotNull(result);
        assertTrue(result.isEmpty(), "При ошибке парсинга должен возвращаться пустой map");
    }
}