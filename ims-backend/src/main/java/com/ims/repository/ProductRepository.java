package com.ims.repository;

import com.ims.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findBySku(String sku);
    Optional<Product> findBySkuAndTenantId(String sku, String tenantId);

    boolean existsBySku(String sku);
    boolean existsBySkuAndTenantId(String sku, String tenantId);
    Optional<Product> findByIdAndTenantId(String id, String tenantId);

    Page<Product> findByVendorSuppliesVendorId(String vendorId, Pageable pageable);
    Page<Product> findByTenantIdAndVendorSuppliesVendorId(String tenantId, String vendorId, Pageable pageable);

    @Query("{ $and: [ " +
           "{ 'tenantId': ?0 }, " +
           "{ $or: [ { 'category': ?1 }, { $expr: { $eq: [?1, null] } } ] }, " +
           "{ $or: [ { 'vendorSupplies.vendorId': ?2 }, { $expr: { $eq: [?2, null] } } ] }, " +
           "{ $or: [ { 'name': { $regex: ?3, $options: 'i' } }, { $expr: { $eq: [?3, null] } } ] } " +
           "] }")
    Page<Product> findWithFilters(String tenantId, String category, String vendorId, String name, Pageable pageable);
}
