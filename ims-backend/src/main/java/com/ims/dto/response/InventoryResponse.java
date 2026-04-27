package com.ims.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InventoryResponse {
    private String id;
    private String productId;
    private String productName;
    private String productSku;
    private Integer quantityAvailable;
    private BigDecimal sellingPrice;
    private Integer reorderLevel;
    private boolean lowStock;
    private Long totalPurchased;
    private Long totalSold;
    private LocalDateTime updatedAt;
}
