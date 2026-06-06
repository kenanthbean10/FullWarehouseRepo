package com.example.goldenhosewarehouse.api.web;

import com.example.goldenhosewarehouse.dal.domain.DataSourceEntity;
import com.example.goldenhosewarehouse.dal.service.DataService;
import com.example.goldenhosewarehouse.dal.service.DataSourceService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sources")
public class DataSourceController {

private final DataSourceService dataSourceService;


    public DataSourceController( DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }
/**
 * [Q3] Endpoint to list all data source provider IDs
 * URL: GET /api/sources
 */
@GetMapping
public List<String> getAllSources() {
    return dataSourceService.getAllSourceIdentifiers();
}

    @GetMapping("/{id}")
    public DataSourceEntity getSourceById(@PathVariable String id) {
        return dataSourceService.getSourceById(id);
    }

    // DataSourceController.java
    @PostMapping
    public DataSourceEntity createSource(@RequestBody DataSourceEntity source) {
        return dataSourceService.createSource(source);
    }

    // Inside  @RestController class
    @GetMapping("/pagedresources")
    public ResponseEntity<Slice<DataSourceEntity>> getAllSourcesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(dataSourceService.getAllSourcesPaged(pageable));
    }
    @PutMapping("/{id}")
    public ResponseEntity<DataSourceEntity> updateDataSource(@PathVariable String id, @RequestBody DataSourceEntity source) {
        return ResponseEntity.ok(dataSourceService.updateDataSource(id, source));
    }




}
