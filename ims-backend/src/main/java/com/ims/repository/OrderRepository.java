package com.ims.repository;

import com.ims.entity.Order;
import com.ims.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OrderRepository extends MongoRepository<Order, String> {
    Optional<Order> findByOrderReference(String orderReference);
    Optional<Order> findByTenantIdAndOrderReference(String tenantId, String orderReference);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    Optional<Order> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    Page<Order> findByCreatedByUserId(String createdByUserId, Pageable pageable);
    Page<Order> findByTenantIdAndCreatedByUserId(String tenantId, String createdByUserId, Pageable pageable);

    Page<Order> findByVendorId(String vendorId, Pageable pageable);
    Page<Order> findByTenantIdAndVendorId(String tenantId, String vendorId, Pageable pageable);

    Page<Order> findByVendorIdAndStatus(String vendorId, OrderStatus status, Pageable pageable);
    Page<Order> findByTenantIdAndVendorIdAndStatus(String tenantId, String vendorId, OrderStatus status, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findByTenantIdAndStatus(String tenantId, OrderStatus status, Pageable pageable);
    Page<Order> findByTenantId(String tenantId, Pageable pageable);
}
