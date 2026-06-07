package com.example.producer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataIngestionProducerTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DataIngestionProducer producer;

    @BeforeEach
    void setup() {

        ReflectionTestUtils.setField(
                producer,
                "apiKey",
                "test-api-key"
        );

        ReflectionTestUtils.setField(
                producer,
                "queueName",
                "etl-queue"
        );
    }

    @Test
    void shouldLoadAssetsFromFile() {

        ByteArrayResource resource = new ByteArrayResource("""
                AAPL
                MSFT
                GOOGL
                """.getBytes());

        ReflectionTestUtils.setField(
                producer,
                "assetsFile",
                resource
        );

        producer.loadAssetsFromFile();

        List<String> assets =
                (List<String>) ReflectionTestUtils.getField(
                        producer,
                        "assets"
                );

        assertNotNull(assets);
        assertEquals(3, assets.size());
        assertTrue(assets.contains("AAPL"));
        assertTrue(assets.contains("MSFT"));
        assertTrue(assets.contains("GOOGL"));
    }

    @Test
    void shouldFallbackToAAPLWhenFileLoadingFails() {

        producer.loadAssetsFromFile();

        List<String> assets =
                (List<String>) ReflectionTestUtils.getField(
                        producer,
                        "assets"
                );

        assertNotNull(assets);
        assertEquals(1, assets.size());
        assertEquals("AAPL", assets.get(0));
    }

    @Test
    void shouldSendMessageToRabbitMQ() {

        Map<String, Object> response = new HashMap<>();
        response.put("datatable", Map.of());

        Map<String, Object> meta = new HashMap<>();
        meta.put("next_cursor_id", null);

        response.put("meta", meta);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(response);

        ReflectionTestUtils.invokeMethod(
                producer,
                "processTickerExtraction",
                "AAPL"
        );

        verify(rabbitTemplate, times(1))
                .convertAndSend(
                        eq("etl-queue"),
                        eq(response)
                );
    }

    @Test
    void shouldHandlePaginationCorrectly() {

        Map<String, Object> page1 = new HashMap<>();
        page1.put("datatable", Map.of());

        Map<String, Object> meta1 = new HashMap<>();
        meta1.put("next_cursor_id", "cursor123");
        page1.put("meta", meta1);

        Map<String, Object> page2 = new HashMap<>();
        page2.put("datatable", Map.of());

        Map<String, Object> meta2 = new HashMap<>();
        meta2.put("next_cursor_id", null);
        page2.put("meta", meta2);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(page1)
                .thenReturn(page2);

        ReflectionTestUtils.invokeMethod(
                producer,
                "processTickerExtraction",
                "AAPL"
        );

        verify(restTemplate, times(2))
                .getForObject(anyString(), eq(Map.class));

        verify(rabbitTemplate, times(2))
                .convertAndSend(
                        eq("etl-queue"),
                        any(Object.class)
                );
    }

    @Test
    void shouldStopWhenNoDatatableReturned() {

        Map<String, Object> response = new HashMap<>();

        Map<String, Object> meta = new HashMap<>();
        meta.put("next_cursor_id", null);

        response.put("meta", meta);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(response);

        ReflectionTestUtils.invokeMethod(
                producer,
                "processTickerExtraction",
                "AAPL"
        );

        verify(rabbitTemplate, never())
                .convertAndSend(
                        anyString(),
                        any(Object.class)
                );
    }

    @Test
    void shouldHandleApiFailureGracefully() {

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Nasdaq API Down"));

        assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(
                        producer,
                        "processTickerExtraction",
                        "AAPL"
                )
        );

        verify(rabbitTemplate, never())
                .convertAndSend(
                        anyString(),
                        any(Object.class)
                );
    }

    @Test
    void shouldRunScheduledProcessForAllAssets() {

        ReflectionTestUtils.setField(
                producer,
                "assets",
                Arrays.asList(
                        "AAPL",
                        "MSFT",
                        "GOOGL"
                )
        );

        Map<String, Object> response = new HashMap<>();
        response.put("datatable", Map.of());

        Map<String, Object> meta = new HashMap<>();
        meta.put("next_cursor_id", null);

        response.put("meta", meta);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(response);

        producer.startIngestionProcess();

        verify(rabbitTemplate, times(3))
                .convertAndSend(
                        eq("etl-queue"),
                        any(Object.class)
                );
    }
}
