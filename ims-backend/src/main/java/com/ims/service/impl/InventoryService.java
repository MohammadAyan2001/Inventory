package com.ims.service.impl;

import com.ims.dto.request.InventoryRequest;
import com.ims.dto.request.SellRequest;
import com.ims.dto.response.InventoryResponse;
import com.ims.entity.Inventory;
import com.ims.entity.Product;
import com.ims.exception.DuplicateResourceException;
import com.ims.exception.InsufficientStockException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.kafka.InventoryUpdateEvent;
import com.ims.kafka.producer.EventProducer;
import com.ims.repository.InventoryRepository;
import com.ims.tenant.TenantResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductService productService;
    private final EventProducer eventProducer;
    private final TenantResolver tenantResolver;

    public InventoryResponse create(InventoryRequest request) {
        String tenantId = currentTenantId();
        if (inventoryRepository.findByTenantIdAndProductId(tenantId, request.getProductId()).isPresent()) {
            throw new DuplicateResourceException("Inventory already exists for product: " + request.getProductId());
        }

        Product product = productService.findById(request.getProductId());

        Inventory inventory = Inventory.builder()
            .tenantId(tenantId)
            .productId(product.getId())
            .productSku(product.getSku())
            .productName(product.getName())
            .quantityAvailable(request.getQuantityAvailable())
            .sellingPrice(request.getSellingPrice())
            .reorderLevel(request.getReorderLevel() == null ? 10 : request.getReorderLevel())
            .totalPurchased(request.getQuantityAvailable())
            .totalSold(0)
            .updatedAt(LocalDateTime.now())
            .build();

        Inventory saved = inventoryRepository.save(inventory);
        publishEvent(saved, 0, saved.getQuantityAvailable(), "CREATE");
        return toResponse(saved);
    }

    @Cacheable(value = "inventory", key = "#id")
    public InventoryResponse getById(String id) {
        return toResponse(findById(id));
    }

    public InventoryResponse getByProduct(String productId) {
        return toResponse(findByProductId(productId));
    }

    public List<InventoryResponse> getAll(String productName) {
        String tenantId = currentTenantId();
        List<Inventory> inventories;
        if (productName != null && !productName.isBlank()) {
            inventories = inventoryRepository.findByTenantIdAndProductNameContainingIgnoreCase(tenantId, productName);
        } else {
            inventories = inventoryRepository.findByTenantId(tenantId);
        }
        return inventories.stream().map(this::toResponse).toList();
    }

    public List<InventoryResponse> getLowStockItems() {
        return inventoryRepository.findLowStockItems(currentTenantId()).stream().map(this::toResponse).toList();
    }

    @CacheEvict(value = "inventory", key = "#id")
    public InventoryResponse restock(String id, int quantity) {
        Inventory inventory = findById(id);
        int previous = inventory.getQuantityAvailable();
        inventory.setQuantityAvailable(previous + quantity);
        inventory.setTotalPurchased(inventory.getTotalPurchased() + quantity);
        inventory.setUpdatedAt(LocalDateTime.now());

        Inventory saved = inventoryRepository.save(inventory);
        publishEvent(saved, previous, saved.getQuantityAvailable(), "RESTOCK");
        return toResponse(saved);
    }

    public InventoryResponse updateSellingPrice(String productId, BigDecimal sellingPrice) {
        Inventory inventory = findByProductId(productId);
        inventory.setSellingPrice(sellingPrice);
        inventory.setUpdatedAt(LocalDateTime.now());
        return toResponse(inventoryRepository.save(inventory));
    }

    public InventoryResponse receiveStock(String productId, int quantity, BigDecimal sellingPrice, Integer reorderLevel) {
        Product product = productService.findById(productId);
        String tenantId = currentTenantId();

        Inventory inventory = inventoryRepository.findByTenantIdAndProductId(tenantId, productId)
            .orElseGet(() -> Inventory.builder()
                .tenantId(tenantId)
                .productId(product.getId())
                .productSku(product.getSku())
                .productName(product.getName())
                .quantityAvailable(0)
                .sellingPrice(sellingPrice)
                .reorderLevel(reorderLevel == null ? 10 : reorderLevel)
                .totalPurchased(0)
                .totalSold(0)
                .updatedAt(LocalDateTime.now())
                .build());

        int previous = inventory.getQuantityAvailable();
        inventory.setQuantityAvailable(previous + quantity);
        inventory.setTotalPurchased(inventory.getTotalPurchased() + quantity);

        if (sellingPrice != null) {
            inventory.setSellingPrice(sellingPrice);
        }
        if (reorderLevel != null) {
            inventory.setReorderLevel(reorderLevel);
        }

        inventory.setUpdatedAt(LocalDateTime.now());
        Inventory saved = inventoryRepository.save(inventory);
        publishEvent(saved, previous, saved.getQuantityAvailable(), "RECEIVE");
        return toResponse(saved);
    }

    public InventoryResponse sell(SellRequest request) {
        Inventory inventory = findByProductId(request.getProductId());
        int available = inventory.getQuantityAvailable();

        if (available < request.getQuantity()) {
            throw new InsufficientStockException(inventory.getProductSku(), request.getQuantity(), available);
        }

        inventory.setQuantityAvailable(available - request.getQuantity());
        inventory.setTotalSold(inventory.getTotalSold() + request.getQuantity());
        inventory.setUpdatedAt(LocalDateTime.now());

        Inventory saved = inventoryRepository.save(inventory);
        publishEvent(saved, available, saved.getQuantityAvailable(), "SALE");
        return toResponse(saved);
    }

    private Inventory findById(String id) {
        return inventoryRepository.findByIdAndTenantId(id, currentTenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + id));
    }

    private Inventory findByProductId(String productId) {
        return inventoryRepository.findByTenantIdAndProductId(currentTenantId(), productId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
    }

    private void publishEvent(Inventory inventory, int previous, int current, String type) {
        eventProducer.publishInventoryUpdate(InventoryUpdateEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .tenantId(inventory.getTenantId())
            .inventoryId(inventory.getId())
            .productId(inventory.getProductId())
            .productSku(inventory.getProductSku())
            .warehouseId(null)
            .previousQuantity(previous)
            .newQuantity(current)
            .updateType(type)
            .occurredAt(LocalDateTime.now())
            .build());
    }

    public InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
            .id(inventory.getId())
            .productId(inventory.getProductId())
            .productName(inventory.getProductName())
            .productSku(inventory.getProductSku())
            .quantityAvailable(inventory.getQuantityAvailable())
            .sellingPrice(inventory.getSellingPrice())
            .reorderLevel(inventory.getReorderLevel())
            .lowStock(inventory.getQuantityAvailable() <= inventory.getReorderLevel())
            .totalPurchased(inventory.getTotalPurchased())
            .totalSold(inventory.getTotalSold())
            .updatedAt(inventory.getUpdatedAt())
            .build();
    }

    private String currentTenantId() {
        return tenantResolver.currentTenantId();
    }
}
