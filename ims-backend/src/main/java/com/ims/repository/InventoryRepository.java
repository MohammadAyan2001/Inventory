package com.ims.repository;

import com.ims.entity.Inventory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends MongoRepository<Inventory, String> {
    Optional<Inventory> findByProductId(String productId);
    Optional<Inventory> findByTenantIdAndProductId(String tenantId, String productId);
    Optional<Inventory> findByIdAndTenantId(String id, String tenantId);
    boolean existsByIdAndTenantId(String id, String tenantId);

    List<Inventory> findByProductNameContainingIgnoreCase(String productName);
    List<Inventory> findByTenantIdAndProductNameContainingIgnoreCase(String tenantId, String productName);
    List<Inventory> findByTenantId(String tenantId);

    @Query("{ $and: [ { 'tenantId': ?0 }, { $expr: { $lte: [ '$quantityAvailable', '$reorderLevel' ] } } ] }")
    List<Inventory> findLowStockItems(String tenantId);
}
