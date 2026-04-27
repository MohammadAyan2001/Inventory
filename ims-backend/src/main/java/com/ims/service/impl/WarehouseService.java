package com.ims.service.impl;

import com.ims.dto.request.WarehouseRequest;
import com.ims.entity.Warehouse;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.WarehouseRepository;
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
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final TenantResolver tenantResolver;

    public Warehouse create(WarehouseRequest request) {
        return warehouseRepository.save(Warehouse.builder()
            .tenantId(currentTenantId())
            .name(request.getName()).location(request.getLocation())
            .capacity(request.getCapacity()).createdAt(LocalDateTime.now()).build());
    }

    @Cacheable(value = "warehouses", key = "#id")
    public Warehouse getById(String id) {
        return warehouseRepository.findByIdAndTenantId(id, currentTenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));
    }

    public Page<Warehouse> getAll(String location, Pageable pageable) {
        String tenantId = currentTenantId();
        return (location != null && !location.isBlank())
            ? warehouseRepository.findByTenantIdAndLocationContainingIgnoreCase(tenantId, location, pageable)
            : warehouseRepository.findByTenantId(tenantId, pageable);
    }

    @CacheEvict(value = "warehouses", key = "#id")
    public Warehouse update(String id, WarehouseRequest request) {
        Warehouse wh = getById(id);
        wh.setName(request.getName());
        wh.setLocation(request.getLocation());
        wh.setCapacity(request.getCapacity());
        return warehouseRepository.save(wh);
    }

    @CacheEvict(value = "warehouses", key = "#id")
    public void delete(String id) {
        Warehouse warehouse = getById(id);
        warehouseRepository.delete(warehouse);
    }

    private String currentTenantId() {
        return tenantResolver.currentTenantId();
    }
}
