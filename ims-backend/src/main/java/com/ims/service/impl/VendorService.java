package com.ims.service.impl;

import com.ims.dto.request.VendorRequest;
import com.ims.entity.Vendor;
import com.ims.exception.DuplicateResourceException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.VendorRepository;
import com.ims.tenant.TenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final TenantResolver tenantResolver;

    public Vendor create(VendorRequest request) {
        String tenantId = currentTenantId();
        if (vendorRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            throw new DuplicateResourceException("Vendor email already exists: " + request.getEmail());
        }
        return vendorRepository.save(Vendor.builder()
            .tenantId(tenantId)
            .name(request.getName()).email(request.getEmail())
            .phone(request.getPhone()).address(request.getAddress())
            .createdAt(LocalDateTime.now()).build());
    }

    @Cacheable(value = "vendors", key = "#id")
    public Vendor getById(String id) {
        return vendorRepository.findByIdAndTenantId(id, currentTenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + id));
    }

    public Page<Vendor> getAll(String name, Pageable pageable) {
        String tenantId = currentTenantId();
        return (name != null && !name.isBlank())
            ? vendorRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, name, pageable)
            : vendorRepository.findByTenantId(tenantId, pageable);
    }

    @CacheEvict(value = "vendors", key = "#id")
    public Vendor update(String id, VendorRequest request) {
        Vendor vendor = getById(id);
        vendor.setName(request.getName());
        vendor.setPhone(request.getPhone());
        vendor.setAddress(request.getAddress());
        return vendorRepository.save(vendor);
    }

    @CacheEvict(value = "vendors", key = "#id")
    public void delete(String id) {
        Vendor vendor = getById(id);
        vendorRepository.delete(vendor);
    }

    private String currentTenantId() {
        return tenantResolver.currentTenantId();
    }
}
