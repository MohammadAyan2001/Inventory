package com.ims.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SellRequest {
    @NotBlank
    private String productId;

    @NotNull
    @Positive
    private Integer quantity;
}
