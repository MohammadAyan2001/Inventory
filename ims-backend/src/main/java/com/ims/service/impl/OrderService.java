package com.ims.service.impl;

import com.ims.dto.request.OrderRequest;
import com.ims.dto.request.ReceiveOrderRequest;
import com.ims.dto.response.OrderResponse;
import com.ims.entity.Order;
import com.ims.entity.OrderItem;
import com.ims.entity.Product;
import com.ims.entity.User;
import com.ims.entity.Vendor;
import com.ims.enums.OrderStatus;
import com.ims.exception.ResourceNotFoundException;
import com.ims.kafka.OrderPlacedEvent;
import com.ims.kafka.producer.EventProducer;
import com.ims.repository.OrderRepository;
import com.ims.tenant.TenantResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final VendorService vendorService;
    private final InventoryService inventoryService;
    private final EventProducer eventProducer;
    private final TenantResolver tenantResolver;

    public OrderResponse placeOrder(OrderRequest request) {
        String tenantId = currentTenantId();
        return orderRepository.findByTenantIdAndIdempotencyKey(tenantId, request.getIdempotencyKey())
            .map(this::toResponse)
            .orElseGet(() -> createNewOrder(request));
    }

    private OrderResponse createNewOrder(OrderRequest request) {
        User currentUser = getCurrentUser();
        Vendor vendor = vendorService.getById(request.getVendorId());

        List<OrderItem> items = request.getItems().stream().map(itemRequest -> {
            Product product = productService.findById(itemRequest.getProductId());
            BigDecimal vendorPrice = productService.resolveVendorPrice(product, vendor.getId(), itemRequest.getVendorPrice());
            BigDecimal subtotal = vendorPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            return OrderItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productSku(product.getSku())
                .quantity(itemRequest.getQuantity())
                .vendorPrice(vendorPrice)
                .subtotal(subtotal)
                .build();
        }).toList();

        BigDecimal total = items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
            .tenantId(currentTenantId())
            .orderReference(Order.generateReference())
            .idempotencyKey(request.getIdempotencyKey())
            .createdByUserId(currentUser.getId())
            .createdByEmail(currentUser.getEmail())
            .vendorId(vendor.getId())
            .vendorName(vendor.getName())
            .status(OrderStatus.CREATED)
            .totalAmount(total)
            .items(items)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        Order saved = orderRepository.save(order);
        eventProducer.publishOrderPlaced(buildEvent(saved));
        log.info("Purchase order created: {}", saved.getOrderReference());
        return toResponse(saved);
    }

    public OrderResponse getByReference(String reference) {
        return toResponse(getOrder(reference));
    }

    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        User user = getCurrentUser();
        return orderRepository.findByTenantIdAndCreatedByUserId(currentTenantId(), user.getId(), pageable)
            .map(this::toResponse);
    }

    public Page<OrderResponse> getVendorIncomingOrders(OrderStatus status, Pageable pageable) {
        String vendorId = getCurrentVendorId();
        String tenantId = currentTenantId();
        if (status == null) {
            return orderRepository.findByTenantIdAndVendorId(tenantId, vendorId, pageable).map(this::toResponse);
        }
        return orderRepository.findByTenantIdAndVendorIdAndStatus(tenantId, vendorId, status, pageable)
            .map(this::toResponse);
    }

    public Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        String tenantId = currentTenantId();
        if (status == null) {
            return orderRepository.findByTenantId(tenantId, pageable).map(this::toResponse);
        }
        return orderRepository.findByTenantIdAndStatus(tenantId, status, pageable).map(this::toResponse);
    }

    public OrderResponse acceptOrder(String reference) {
        Order order = getOrder(reference);
        assertVendorOwnership(order);
        transition(order, OrderStatus.CREATED, OrderStatus.ACCEPTED);
        return saveAndMap(order);
    }

    public OrderResponse shipOrder(String reference) {
        Order order = getOrder(reference);
        assertVendorOwnership(order);
        transition(order, OrderStatus.ACCEPTED, OrderStatus.SHIPPED);
        return saveAndMap(order);
    }

    public OrderResponse receiveOrder(String reference, ReceiveOrderRequest request) {
        Order order = getOrder(reference);
        transition(order, OrderStatus.SHIPPED, OrderStatus.RECEIVED);

        Map<String, ReceiveOrderRequest.ReceivedItemRequest> sellingConfig = request.getItems().stream()
            .collect(Collectors.toMap(
                ReceiveOrderRequest.ReceivedItemRequest::getProductId,
                Function.identity(),
                (first, second) -> second
            ));

        for (OrderItem item : order.getItems()) {
            ReceiveOrderRequest.ReceivedItemRequest config = sellingConfig.get(item.getProductId());
            if (config == null) {
                throw new IllegalArgumentException("Missing selling price for product " + item.getProductId());
            }
            inventoryService.receiveStock(item.getProductId(), item.getQuantity(), config.getSellingPrice(), config.getReorderLevel());
        }

        return saveAndMap(order);
    }

    public OrderResponse cancelOrder(String reference) {
        Order order = getOrder(reference);
        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot cancel order in status " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    private OrderPlacedEvent buildEvent(Order order) {
        List<OrderPlacedEvent.OrderItemEvent> itemEvents = order.getItems().stream()
            .map(item -> OrderPlacedEvent.OrderItemEvent.builder()
                .productId(item.getProductId())
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .build())
            .toList();

        return OrderPlacedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .tenantId(order.getTenantId())
            .orderReference(order.getOrderReference())
            .userId(order.getCreatedByUserId())
            .status(order.getStatus())
            .totalAmount(order.getTotalAmount())
            .items(itemEvents)
            .occurredAt(LocalDateTime.now())
            .build();
    }

    public OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
            .map(item -> OrderResponse.OrderItemResponse.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .vendorPrice(item.getVendorPrice())
                .subtotal(item.getSubtotal())
                .build())
            .toList();

        return OrderResponse.builder()
            .id(order.getId())
            .orderReference(order.getOrderReference())
            .vendorId(order.getVendorId())
            .vendorName(order.getVendorName())
            .createdByEmail(order.getCreatedByEmail())
            .status(order.getStatus())
            .totalAmount(order.getTotalAmount())
            .items(itemResponses)
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .build();
    }

    private Order getOrder(String reference) {
        return orderRepository.findByTenantIdAndOrderReference(currentTenantId(), reference)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + reference));
    }

    private OrderResponse saveAndMap(Order order) {
        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    private void transition(Order order, OrderStatus from, OrderStatus to) {
        if (order.getStatus() != from) {
            throw new IllegalStateException(
                "Invalid transition. Expected " + from + " but current status is " + order.getStatus());
        }
        order.setStatus(to);
    }

    private void assertVendorOwnership(Order order) {
        String vendorId = getCurrentVendorId();
        if (!vendorId.equals(order.getVendorId())) {
            throw new AccessDeniedException("This order does not belong to the current vendor");
        }
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
