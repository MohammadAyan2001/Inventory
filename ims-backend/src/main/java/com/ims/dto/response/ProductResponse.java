package com.ims.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private String sku;
    private String description;
    private String category;
    private List<VendorSupplyResponse> vendorSupplies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class VendorSupplyResponse {
        private String vendorId;
        private String vendorName;
        private BigDecimal vendorPrice;
        private LocalDateTime updatedAt;
    }
}
