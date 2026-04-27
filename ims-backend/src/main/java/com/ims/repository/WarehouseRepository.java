package com.ims.repository;

import com.ims.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface WarehouseRepository extends MongoRepository<Warehouse, String> {
    Page<Warehouse> findByLocationContainingIgnoreCase(String location, Pageable pageable);
    Page<Warehouse> findByTenantIdAndLocationContainingIgnoreCase(String tenantId, String location, Pageable pageable);
    Page<Warehouse> findByTenantId(String tenantId, Pageable pageable);
    boolean existsByIdAndTenantId(String id, String tenantId);
    Optional<Warehouse> findByIdAndTenantId(String id, String tenantId);
}
