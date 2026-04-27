package com.ims.repository;

import com.ims.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface VendorRepository extends MongoRepository<Vendor, String> {
    Optional<Vendor> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Vendor> findByEmailAndTenantId(String email, String tenantId);
    boolean existsByEmailAndTenantId(String email, String tenantId);
    Optional<Vendor> findByIdAndTenantId(String id, String tenantId);
    Page<Vendor> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Vendor> findByTenantIdAndNameContainingIgnoreCase(String tenantId, String name, Pageable pageable);
    Page<Vendor> findByTenantId(String tenantId, Pageable pageable);
    boolean existsByIdAndTenantId(String id, String tenantId);
}
