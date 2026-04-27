package com.ims.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
    @CompoundIndex(name = "uniq_tenant_sku", def = "{'tenantId': 1, 'sku': 1}", unique = true),
    @CompoundIndex(name = "idx_tenant_category", def = "{'tenantId': 1, 'category': 1}")
})
public class Product {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String name;

    @Indexed
    private String sku;

    private String description;
    private String category;

    @Builder.Default
    private List<VendorSupply> vendorSupplies = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
