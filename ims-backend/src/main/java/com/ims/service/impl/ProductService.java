package com.ims.service.impl;

import com.ims.dto.request.ProductRequest;
import com.ims.dto.request.VendorProductRequest;
import com.ims.dto.response.ProductResponse;
import com.ims.entity.Product;
import com.ims.entity.User;
import com.ims.entity.Vendor;
import com.ims.entity.VendorSupply;
import com.ims.exception.DuplicateResourceException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.ProductRepository;
import com.ims.tenant.TenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final VendorService vendorService;
    private final TenantResolver tenantResolver;

    public ProductResponse create(ProductRequest request) {
        String tenantId = currentTenantId();
        if (productRepository.existsBySkuAndTenantId(request.getSku(), tenantId)) {
            throw new DuplicateResourceException("SKU already exists: " + request.getSku());
        }

        Product product = Product.builder()
            .tenantId(tenantId)
            .name(request.getName())
            .sku(request.getSku())
            .description(request.getDescription())
            .category(request.getCategory())
            .vendorSupplies(new ArrayList<>())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        return toResponse(productRepository.save(product));
    }

    public ProductResponse addSupplyForCurrentVendor(VendorProductRequest request) {
        Vendor vendor = getCurrentVendor();
        String tenantId = currentTenantId();

        Product product;
        if (request.getExistingProductId() != null && !request.getExistingProductId().isBlank()) {
            product = findById(request.getExistingProductId());
        } else if (request.getSku() != null && !request.getSku().isBlank()) {
            product = productRepository.findBySkuAndTenantId(request.getSku(), tenantId)
                .orElseGet(() -> createBaseProductFromVendorRequest(request));
        } else {
            throw new IllegalArgumentException("Either existingProductId or sku is required");
        }

        upsertVendorSupply(product, vendor, request.getVendorPrice());
        product.setUpdatedAt(LocalDateTime.now());
        return toResponse(productRepository.save(product));
    }

    public Page<ProductResponse> getCurrentVendorProducts(Pageable pageable) {
        String vendorId = getCurrentVendorId();
        return productRepository.findByTenantIdAndVendorSuppliesVendorId(currentTenantId(), vendorId, pageable)
            .map(this::toResponse);
    }

    public ProductResponse updateCurrentVendorPrice(String productId, BigDecimal vendorPrice) {
        Vendor vendor = getCurrentVendor();
        Product product = findById(productId);
        upsertVendorSupply(product, vendor, vendorPrice);
        product.setUpdatedAt(LocalDateTime.now());
        return toResponse(productRepository.save(product));
    }

    @Cacheable(value = "products", key = "#id")
    public ProductResponse getById(String id) {
        return toResponse(findById(id));
    }

    public Product findById(String id) {
        return productRepository.findByIdAndTenantId(id, currentTenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public Page<ProductResponse> getAll(String category, String vendorId, String name, Pageable pageable) {
        String queryName = name == null ? "" : name;
        return productRepository.findWithFilters(currentTenantId(), category, vendorId, queryName, pageable)
            .map(this::toResponse);
    }

    @CacheEvict(value = "products", key = "#id")
    public ProductResponse update(String id, ProductRequest request) {
        Product product = findById(id);

        if (!product.getSku().equalsIgnoreCase(request.getSku())
            && productRepository.existsBySkuAndTenantId(request.getSku(), currentTenantId())) {
            throw new DuplicateResourceException("SKU already exists: " + request.getSku());
        }

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setUpdatedAt(LocalDateTime.now());

        return toResponse(productRepository.save(product));
    }

    @CacheEvict(value = "products", key = "#id")
    public void delete(String id) {
        Product product = findById(id);
        productRepository.delete(product);
    }

    public BigDecimal resolveVendorPrice(Product product, String vendorId, BigDecimal requestedVendorPrice) {
        if (requestedVendorPrice != null) {
            return requestedVendorPrice;
        }

        return product.getVendorSupplies().stream()
            .filter(s -> s.getVendorId().equals(vendorId))
            .findFirst()
            .map(VendorSupply::getVendorPrice)
            .orElseThrow(() -> new IllegalArgumentException(
                "Vendor " + vendorId + " does not supply product " + product.getName()));
    }

    public ProductResponse toResponse(Product p) {
        List<ProductResponse.VendorSupplyResponse> supplies = p.getVendorSupplies().stream()
            .map(s -> ProductResponse.VendorSupplyResponse.builder()
                .vendorId(s.getVendorId())
                .vendorName(s.getVendorName())
                .vendorPrice(s.getVendorPrice())
                .updatedAt(s.getUpdatedAt())
                .build())
            .toList();

        return ProductResponse.builder()
            .id(p.getId())
            .name(p.getName())
            .sku(p.getSku())
            .description(p.getDescription())
            .category(p.getCategory())
            .vendorSupplies(supplies)
            .createdAt(p.getCreatedAt())
            .updatedAt(p.getUpdatedAt())
            .build();
    }

    private Product createBaseProductFromVendorRequest(VendorProductRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Product name is required when creating a new product");
        }

        Product product = Product.builder()
            .tenantId(currentTenantId())
            .name(request.getName())
            .sku(request.getSku())
            .description(request.getDescription())
            .category(request.getCategory())
            .vendorSupplies(new ArrayList<>())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        return productRepository.save(product);
    }

    private void upsertVendorSupply(Product product, Vendor vendor, BigDecimal vendorPrice) {
        VendorSupply supply = product.getVendorSupplies().stream()
            .filter(s -> s.getVendorId().equals(vendor.getId()))
            .findFirst()
            .orElseGet(() -> {
                VendorSupply created = VendorSupply.builder().vendorId(vendor.getId()).build();
                product.getVendorSupplies().add(created);
                return created;
            });

        supply.setVendorName(vendor.getName());
        supply.setVendorPrice(vendorPrice);
        supply.setUpdatedAt(LocalDateTime.now());
    }

    private Vendor getCurrentVendor() {
        String vendorId = getCurrentVendorId();
        return vendorService.getById(vendorId);
    }

    private String getCurrentVendorId() {
        User user = getCurrentUser();
        if (user.getVendorId() == null || user.getVendorId().isBlank()) {
            throw new AccessDeniedException("Vendor profile not linked to current user");
        }
        return user.getVendorId();
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User user)) {
            throw new AccessDeniedException("Invalid authenticated principal");
        }
        return user;
    }

    private String currentTenantId() {
        return tenantResolver.currentTenantId();
    }
}
