package com.ims.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
    @CompoundIndex(name = "uniq_tenant_product_inventory", def = "{'tenantId': 1, 'productId': 1}", unique = true)
})
public class Inventory {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String productId;

    private String productSku;
    private String productName;

    private int quantityAvailable;

    private BigDecimal sellingPrice;

    private int reorderLevel;

    private long totalPurchased;
    private long totalSold;

    private LocalDateTime updatedAt;
}
