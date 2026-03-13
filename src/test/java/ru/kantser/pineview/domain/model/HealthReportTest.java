package ru.kantser.pineview.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HealthReportTest {

    @Test
    void testConstructorAndGetters() {
        // Arrange
        String name = "PineconeService";
        String url = "https://api.pinecone.io";
        ServiceStatus status = ServiceStatus.ONLINE;
        long time = 150L;
        String msg = "OK";

        // Act
        HealthReport report = new HealthReport(name, url, status, time, msg);

        // Assert
        assertEquals(name, report.getServiceName());
        assertEquals(url, report.getUrl());
        assertEquals(status, report.getStatus());
        assertEquals(time, report.getResponseTimeMs());
        assertEquals(msg, report.getMessage());
    }
    
    @Test
    void testEquality() {
        // Проверка равенства (если в классе переопределен equals, иначе это проверка ссылок)
        // Для простых POJO часто проверяют, что два объекта с одинаковыми данными не равны (разные ссылки),
        // или наоборот, если используется Lombok @EqualsAndHashCode.
        // В вашем классе equals не переопределен, поэтому проверим уникальность.
        
        HealthReport r1 = new HealthReport("s1", "u1", ServiceStatus.ONLINE, 1L, "m1");
        HealthReport r2 = new HealthReport("s1", "u1", ServiceStatus.ONLINE, 1L, "m1");
        
        assertNotSame(r1, r2); // Это разные объекты
    }
}