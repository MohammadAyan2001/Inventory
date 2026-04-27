package com.ims.entity;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    private String productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private BigDecimal vendorPrice;
    private BigDecimal subtotal;
}
