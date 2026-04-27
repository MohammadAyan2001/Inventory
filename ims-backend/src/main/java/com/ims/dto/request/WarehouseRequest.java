package com.ims.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class WarehouseRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String location;

    @Positive
    private Integer capacity;
}
