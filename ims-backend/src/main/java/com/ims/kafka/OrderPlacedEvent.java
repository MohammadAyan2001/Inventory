package com.ims.kafka;

import com.ims.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderPlacedEvent {
    private String eventId;
    private String tenantId;
    private String orderReference;
    private String userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItemEvent> items;
    private LocalDateTime occurredAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemEvent {
        private String productId;
        private String productSku;
        private Integer quantity;
    }
}
