package ru.kantser.pineview.adapter.http;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.kantser.pineview.domain.model.HealthReport;
import ru.kantser.pineview.domain.model.ServiceStatus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class OkHttpHealthCheckerTest {

    private MockWebServer mockWebServer;
    private OkHttpHealthChecker healthChecker;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Запускаем локальный эмулятор сервера
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // 2. Создаем экземпляр проверяющего класса
        healthChecker = new OkHttpHealthChecker();
    }

    @AfterEach
    void tearDown() throws Exception {
        // 3. Закрываем сервер после каждого теста
        mockWebServer.shutdown();
    }

    @Test
    void checkHealth_ShouldReturnOnline_WhenResponseIs200() throws Exception {
        // Arrange (Подготовка)
        // Настраиваем сервер вернуть код 200 OK
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // Получаем URL мок-сервера (например, http://localhost:12345/)
        String url = mockWebServer.url("/").toString();

        // Act (Действие)
        CompletableFuture<HealthReport> future = healthChecker.checkHealth("TestService", url);
        
        // Ждем завершения асинхронной задачи (максимум 2 секунды)
        HealthReport report = future.get(2, TimeUnit.SECONDS);

        // Assert (Проверка)
        assertNotNull(report);
        assertEquals("TestService", report.getServiceName());
        assertEquals(ServiceStatus.ONLINE, report.getStatus(), "Статус должен быть ONLINE");
        assertTrue(report.getResponseTimeMs() >= 0, "Время ответа должно быть >= 0");
        assertEquals("HTTP 200", report.getMessage());
    }

    @Test
    void checkHealth_ShouldReturnOffline_WhenResponseIs500() throws Exception {
        // Arrange
        // Сервер вернет ошибку 500
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        String url = mockWebServer.url("/").toString();

        // Act
        CompletableFuture<HealthReport> future = healthChecker.checkHealth("BrokenService", url);
        HealthReport report = future.get(2, TimeUnit.SECONDS);

        // Assert
        assertNotNull(report);
        assertEquals(ServiceStatus.OFFLINE, report.getStatus(), "Статус должен быть OFFLINE при 500 ошибке");
        assertEquals("HTTP 500", report.getMessage());
    }

    @Test
    void checkHealth_ShouldReturnOffline_WhenNetworkFails() throws Exception {
        // Arrange
        // 1. Получаем валидный URL с мок-сервера
        String url = mockWebServer.url("/").toString();

        // 2. Выключаем сервер, чтобы симулировать падение сети
        mockWebServer.shutdown();

        // Act
        CompletableFuture<HealthReport> future = healthChecker.checkHealth("OfflineService", url);
        HealthReport report = future.get(2, TimeUnit.SECONDS);

        // Assert
        assertNotNull(report);
        assertEquals(ServiceStatus.OFFLINE, report.getStatus(), "При ошибке сети статус должен быть OFFLINE");
        assertNotNull(report.getMessage());
        assertTrue(report.getMessage().length() > 0);
    }
}