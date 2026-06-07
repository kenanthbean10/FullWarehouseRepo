package com.example.goldenhosewarehouse;

import com.example.goldenhosewarehouse.dal.domain.DataEntity;
import com.example.goldenhosewarehouse.dal.persistence.DataRepositoryImpl;
import com.example.goldenhosewarehouse.dal.repository.DataKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.cql.CqlOperations;
import org.springframework.data.cassandra.core.convert.CassandraConverter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataRepositoryImplTest {

    @Mock
    private CassandraTemplate cassandraTemplate;

    @Mock
    private CqlOperations cqlOperations;

    @Mock
    private CassandraConverter converter;

    @InjectMocks
    private DataRepositoryImpl dataRepository;

    private DataEntity testEntity;
    private DataKey testKey;

    @BeforeEach
    void setUp() {
        testKey = new DataKey("AAPL", "NASDAQ_API");

        testEntity = new DataEntity();
        testEntity.setAssetId("AAPL");
        testEntity.setDataSourceId("NASDAQ_API");
        testEntity.setBusinessDate(LocalDate.of(2024, 1, 15));
        testEntity.setSystemDate(Instant.now());
        testEntity.setValuesdouble(Map.of("close", 150.0));
    }

    // ================= SAVE =================
    @Test
    void testSave() {
        when(cassandraTemplate.insert(testEntity)).thenReturn(testEntity);

        DataEntity result = dataRepository.save(testEntity);

        assertThat(result).isEqualTo(testEntity);
        verify(cassandraTemplate).insert(testEntity);
    }

    // ================= FIND LATEST =================
    @Test
    void testFindLatest() {
        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);

        when(cqlOperations.queryForObject(
                anyString(),
                any(org.springframework.data.cassandra.core.cql.RowMapper.class),
                eq("AAPL"),
                eq("NASDAQ_API")
        )).thenReturn(testEntity);

        Optional<DataEntity> result = dataRepository.findLatest(testKey);

        assertThat(result).isPresent();
        assertThat(result.get().getAssetId()).isEqualTo("AAPL");
    }

    // ================= FIND LATEST EMPTY =================
    @Test
    void testFindLatestEmpty() {
        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);

        when(cqlOperations.queryForObject(
                anyString(),
                any(org.springframework.data.cassandra.core.cql.RowMapper.class),
                anyString(),
                anyString()
        )).thenThrow(new RuntimeException("No record"));

        Optional<DataEntity> result = dataRepository.findLatest(testKey);

        assertThat(result).isEmpty();
    }

    // ================= FIND ALL =================
    @Test
    void testFindAllByKey() {
        lenient().when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        lenient().when(cassandraTemplate.getConverter()).thenReturn(converter);

        List<DataEntity> expected = List.of(testEntity);

        lenient().when(cqlOperations.query(
                anyString(),
                any(org.springframework.data.cassandra.core.cql.RowMapper.class),
                anyString(),
                anyString()
        )).thenReturn(expected);

        Iterable<DataEntity> result = dataRepository.findAll(testKey);
        assertThat(result).hasSize(1);
    }

    // ================= RANGE =================
    @Test
    void testFindByRange() {
        lenient().when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        lenient().when(cassandraTemplate.getConverter()).thenReturn(converter);

        List<DataEntity> expected = List.of(testEntity);

        lenient().when(cqlOperations.query(
                anyString(),
                any(org.springframework.data.cassandra.core.cql.RowMapper.class),
                any(), any(), any(), any()
        )).thenReturn(expected);

        Iterable<DataEntity> result = dataRepository.findByRange(
                testKey,
                LocalDate.now().minusDays(1),
                LocalDate.now()
        );
        assertThat(result).hasSize(1);
    }

    // ================= DELETE =================
    @Test
    void testDelete() {
        dataRepository.delete(testEntity);
        verify(cassandraTemplate).delete(testEntity);
    }
}
