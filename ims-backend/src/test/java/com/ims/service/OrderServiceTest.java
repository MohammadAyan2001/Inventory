package com.ims.service;

import com.ims.dto.request.OrderRequest;
import com.ims.dto.response.OrderResponse;
import com.ims.entity.Order;
import com.ims.entity.OrderItem;
import com.ims.entity.Product;
import com.ims.entity.User;
import com.ims.entity.Vendor;
import com.ims.enums.OrderStatus;
import com.ims.enums.Role;
import com.ims.kafka.producer.EventProducer;
import com.ims.repository.OrderRepository;
import com.ims.service.impl.InventoryService;
import com.ims.service.impl.OrderService;
import com.ims.service.impl.ProductService;
import com.ims.service.impl.VendorService;
import com.ims.tenant.TenantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @Mock
    private VendorService vendorService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private EventProducer eventProducer;

    @Mock
    private TenantResolver tenantResolver;

    @InjectMocks
    private OrderService orderService;

    private User adminUser;
    private Product product;
    private Vendor vendor;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        when(tenantResolver.currentTenantId()).thenReturn("public");

        adminUser = User.builder()
            .id("admin-1")
            .email("admin@test.com")
            .name("Admin")
            .role(Role.ADMIN)
            .password("encoded")
            .tenantId("public")
            .build();

        product = Product.builder()
            .id("prod-1")
            .name("Widget")
            .sku("WGT-001")
            .tenantId("public")
            .build();

        vendor = Vendor.builder()
            .id("ven-1")
            .name("Acme Supplies")
            .email("vendor@test.com")
            .tenantId("public")
            .build();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                adminUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            )
        );

        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest();
        itemRequest.setProductId("prod-1");
        itemRequest.setQuantity(5);

        orderRequest = new OrderRequest();
        orderRequest.setIdempotencyKey("idem-001");
        orderRequest.setVendorId("ven-1");
        orderRequest.setItems(List.of(itemRequest));
    }

    @Test
    void placeOrder_returnsExistingOrder_onDuplicateIdempotencyKey() {
        Order existing = Order.builder()
            .id("ord-1")
            .orderReference("PO-EXISTING")
            .idempotencyKey("idem-001")
            .status(OrderStatus.CREATED)
            .totalAmount(BigDecimal.valueOf(50))
            .items(List.of())
            .build();

        when(orderRepository.findByTenantIdAndIdempotencyKey("public", "idem-001")).thenReturn(Optional.of(existing));

        OrderResponse response = orderService.placeOrder(orderRequest);

        assertThat(response.getOrderReference()).isEqualTo("PO-EXISTING");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CREATED);

        verify(productService, never()).findById(any());
        verify(vendorService, never()).getById(any());
        verify(eventProducer, never()).publishOrderPlaced(any());
    }

    @Test
    void placeOrder_createsNewOrder_withVendorPrice_andPublishesEvent() {
        when(orderRepository.findByTenantIdAndIdempotencyKey("public", "idem-001")).thenReturn(Optional.empty());
        when(vendorService.getById("ven-1")).thenReturn(vendor);
        when(productService.findById("prod-1")).thenReturn(product);
        when(productService.resolveVendorPrice(eq(product), eq("ven-1"), eq(null)))
            .thenReturn(BigDecimal.TEN);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order toSave = invocation.getArgument(0);
            toSave.setId("ord-2");
            toSave.setCreatedAt(LocalDateTime.now());
            toSave.setUpdatedAt(LocalDateTime.now());
            return toSave;
        });

        OrderResponse response = orderService.placeOrder(orderRequest);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.getVendorId()).isEqualTo("ven-1");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getVendorPrice()).isEqualByComparingTo("10");
        assertThat(response.getItems().get(0).getSubtotal()).isEqualByComparingTo("50");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("50");

        verify(orderRepository).save(any(Order.class));
        verify(eventProducer).publishOrderPlaced(any());
    }

    @Test
    void placeOrder_throws_whenVendorDoesNotSupplyProduct() {
        when(orderRepository.findByTenantIdAndIdempotencyKey("public", "idem-001")).thenReturn(Optional.empty());
        when(vendorService.getById("ven-1")).thenReturn(vendor);
        when(productService.findById("prod-1")).thenReturn(product);
        when(productService.resolveVendorPrice(eq(product), eq("ven-1"), eq(null)))
            .thenThrow(new IllegalArgumentException("Vendor ven-1 does not supply product Widget"));

        assertThatThrownBy(() -> orderService.placeOrder(orderRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not supply product");

        verify(orderRepository, never()).save(any(Order.class));
        verify(eventProducer, never()).publishOrderPlaced(any());
    }
}
