package com.ims.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VendorProductRequest {

    private String existingProductId;

    @Size(max = 200)
    private String name;

    @Size(max = 50)
    private String sku;

    private String description;
    private String category;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal vendorPrice;
}
