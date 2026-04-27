package com.ims.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReceiveOrderRequest {

    @NotEmpty
    @Valid
    private List<ReceivedItemRequest> items;

    @Data
    public static class ReceivedItemRequest {
        @NotBlank
        private String productId;

        @NotNull
        @DecimalMin("0.01")
        private BigDecimal sellingPrice;

        @Min(0)
        private Integer reorderLevel;
    }
}
