package com.example.goldenetl.mapper;



import com.example.goldenetl.metrics.IngestionPulse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class NasdaqMapperTest {

    @Mock
    private IngestionPulse ingestionPulse;

    private NasdaqMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new NasdaqMapper(ingestionPulse);
    }

    @Test
    void testMappingCorrectness() {

        List<String> columns = Arrays.asList(
                "ticker",
                "date",
                "price",
                "volume",
                "note"
        );

        List<Object> values = Arrays.asList(
                "aapl",
                "2025-01-01",
                150.5,
                1000,
                "test-value"
        );

        Map<String, Object> result = mapper.mapDynamicRow(columns, values);

        // ✅ Core identity
        assertEquals("AAPL", result.get("assetId"));
        assertEquals("NASDAQ_DYNAMIC_V1", result.get("dataSourceId"));
        assertEquals("2025-01-01", result.get("businessDate"));

        // ✅ Double values check
        Map<String, Object> valuesDouble =
                (Map<String, Object>) result.get("valuesdouble");

        assertNotNull(valuesDouble);
        assertEquals(150.5, valuesDouble.get("price"));

        // ✅ Integer values check
        Map<String, Object> valuesInt =
                (Map<String, Object>) result.get("valuesInt");

        assertNotNull(valuesInt);
        assertEquals(1000, valuesInt.get("volume"));

        // ✅ Text values check
        Map<String, Object> valuesText =
                (Map<String, Object>) result.get("valuesText");

        assertNotNull(valuesText);
        assertEquals("test-value", valuesText.get("note"));

        // ✅ Metadata checks
        assertNotNull(result.get("etl_processed_at"));
    }

    @Test
    void testAssetNormalization() {

        List<String> columns = Arrays.asList("ticker", "date", "price");
        List<Object> values = Arrays.asList(" tsla ", "2025-01-01", 200.0);

        Map<String, Object> result = mapper.mapDynamicRow(columns, values);

        // must be uppercase + trimmed
        assertEquals("TSLA", result.get("assetId").toString().trim());
    }

    @Test
    void testNullValuesIgnored() {

        List<String> columns = Arrays.asList("ticker", "date", "price");
        List<Object> values = Arrays.asList("msft", "2025-01-01", null);

        Map<String, Object> result = mapper.mapDynamicRow(columns, values);

        Map<String, Object> valuesDouble =
                (Map<String, Object>) result.get("valuesdouble");

        // null should not crash system
        assertTrue(valuesDouble.isEmpty());
    }
}
