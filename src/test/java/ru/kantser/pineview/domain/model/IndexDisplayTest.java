package ru.kantser.pineview.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.db_control.client.model.IndexModel;
import org.openapitools.db_control.client.model.IndexModelStatus; // Предполагаемый класс статуса

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Включаем Mockito
class IndexDisplayTest {

    @Mock
    private IndexModel mockModel;

    @Mock
    private IndexModelStatus mockStatus;

    @Mock
    private  org.openapitools.db_control.client.model.IndexModelSpec mockSpec;

    @Test
    void testConstructorWithValidModel() {
        // Arrange (Настройка моков)
        when(mockModel.getName()).thenReturn("test-index");
        when(mockModel.getDimension()).thenReturn(1536);
        when(mockModel.getMetric()).thenReturn("cosine");
        
        // Настройка статуса: Ready = true
        when(mockStatus.getReady()).thenReturn(true);
        when(mockModel.getStatus()).thenReturn(mockStatus);
        
        // Настройка Spec
        when(mockModel.getSpec()).thenReturn(mockSpec);
        when(mockSpec.toString()).thenReturn("{\"environment\":\"us-east-1\"}");

        // Act
        IndexDisplay display = new IndexDisplay(mockModel);

        // Assert
        assertEquals("test-index", display.getName());
        assertEquals(1536, display.getDimension());
        assertEquals("cosine", display.getMetric());
        assertEquals("✓ Ready", display.getStatus(), "Статус должен быть Ready");
        assertEquals(-1, display.getVectorCount(), "Vector count должен быть -1 по умолчанию");
    }

    @Test
    void testConstructorWithInitializingStatus() {
        // Arrange
        when(mockModel.getName()).thenReturn("loading-index");
        when(mockModel.getDimension()).thenReturn(512);
        when(mockModel.getMetric()).thenReturn("dotproduct");
        
        // Настройка статуса: Ready = false (или null)
        when(mockStatus.getReady()).thenReturn(false);
        when(mockModel.getStatus()).thenReturn(mockStatus);
        when(mockModel.getSpec()).thenReturn(null);

        // Act
        IndexDisplay display = new IndexDisplay(mockModel);

        // Assert
        assertEquals("⋯ Initializing", display.getStatus());
        assertEquals("serverless", display.getEnvironment(), "Если spec null, должно быть serverless");
    }

    @Test
    void testConstructorWithNullModel() {
        // Arrange
        // Act
        IndexDisplay display = new IndexDisplay(null);

        // Assert
        assertEquals("unknown", display.getName(), "При null модели имя должно быть 'unknown'");
        assertEquals(" Error", display.getStatus(), "При null модели статус должен быть Error");
    }
}