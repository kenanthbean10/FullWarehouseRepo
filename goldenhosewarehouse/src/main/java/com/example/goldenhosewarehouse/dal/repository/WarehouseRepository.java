package com.example.goldenhosewarehouse.dal.repository;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
public interface WarehouseRepository<E, K> {
    E save(E entity);
    void delete(E entity);
    Optional<E> findLatest(K partitionKey);
    Iterable<E> findAll(K partitionKey);
    Slice<E> findAll(Pageable pageable);
}