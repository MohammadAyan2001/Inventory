package com.ims.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorSupply {
    private String vendorId;
    private String vendorName;
    private BigDecimal vendorPrice;
    private LocalDateTime updatedAt;
}
