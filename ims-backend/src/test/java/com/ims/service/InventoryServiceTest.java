package com.ims.service;

import com.ims.dto.request.InventoryRequest;
import com.ims.dto.request.SellRequest;
import com.ims.dto.response.InventoryResponse;
import com.ims.entity.Inventory;
import com.ims.entity.Product;
import com.ims.exception.DuplicateResourceException;
import com.ims.exception.InsufficientStockException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.kafka.producer.EventProducer;
import com.ims.repository.InventoryRepository;
import com.ims.service.impl.InventoryService;
import com.ims.service.impl.ProductService;
import com.ims.tenant.TenantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductService productService;

    @Mock
    private EventProducer eventProducer;

    @Mock
    private TenantResolver tenantResolver;

    @InjectMocks
    private InventoryService inventoryService;

    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        when(tenantResolver.currentTenantId()).thenReturn("public");

        product = Product.builder()
            .id("prod-1")
            .name("Widget")
            .sku("WGT-001")
            .tenantId("public")
            .build();

        inventory = Inventory.builder()
            .id("inv-1")
            .tenantId("public")
            .productId("prod-1")
            .productSku("WGT-001")
            .productName("Widget")
            .quantityAvailable(100)
            .sellingPrice(BigDecimal.valueOf(19.99))
            .reorderLevel(10)
            .totalPurchased(100)
            .totalSold(0)
            .updatedAt(LocalDateTime.now())
            .build();
    }

    @Test
    void create_throwsDuplicate_whenInventoryAlreadyExists() {
        InventoryRequest request = new InventoryRequest();
        request.setProductId("prod-1");
        request.setQuantityAvailable(10);
        request.setSellingPrice(BigDecimal.valueOf(25));

        when(inventoryRepository.findByTenantIdAndProductId("public", "prod-1")).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.create(request))
            .isInstanceOf(DuplicateResourceException.class);

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void restock_increasesQuantityAndPurchased_andPublishesEvent() {
        when(inventoryRepository.findByIdAndTenantId("inv-1", "public")).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventProducer).publishInventoryUpdate(any());

        InventoryResponse response = inventoryService.restock("inv-1", 50);

        assertThat(response.getQuantityAvailable()).isEqualTo(150);
        assertThat(response.getTotalPurchased()).isEqualTo(150);
        verify(eventProducer).publishInventoryUpdate(any());
    }

    @Test
    void sell_reducesAvailable_andIncrementsSold() {
        SellRequest request = new SellRequest();
        request.setProductId("prod-1");
        request.setQuantity(20);

        when(inventoryRepository.findByTenantIdAndProductId("public", "prod-1")).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventProducer).publishInventoryUpdate(any());

        InventoryResponse response = inventoryService.sell(request);

        assertThat(response.getQuantityAvailable()).isEqualTo(80);
        assertThat(response.getTotalSold()).isEqualTo(20);
        verify(eventProducer).publishInventoryUpdate(any());
    }

    @Test
    void sell_throwsInsufficientStock_whenNotEnoughQuantity() {
        SellRequest request = new SellRequest();
        request.setProductId("prod-1");
        request.setQuantity(150);

        when(inventoryRepository.findByTenantIdAndProductId("public", "prod-1")).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.sell(request))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("WGT-001");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        when(inventoryRepository.findByIdAndTenantId("bad-id", "public")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getById("bad-id"))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
