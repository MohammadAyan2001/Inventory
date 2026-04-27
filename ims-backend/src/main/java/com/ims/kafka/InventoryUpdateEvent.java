package com.ims.kafka;

import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryUpdateEvent {
    private String eventId;
    private String tenantId;
    private String inventoryId;
    private String productId;
    private String productSku;
    private String warehouseId;
    private Integer previousQuantity;
    private Integer newQuantity;
    private String updateType;
    private LocalDateTime occurredAt;
}
