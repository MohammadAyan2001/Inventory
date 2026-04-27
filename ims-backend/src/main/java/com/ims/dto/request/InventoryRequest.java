package com.ims.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryRequest {
    @NotBlank
    private String productId;

    @NotNull
    @Min(0)
    private Integer quantityAvailable;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal sellingPrice;

    @Min(0)
    private Integer reorderLevel = 10;
}
