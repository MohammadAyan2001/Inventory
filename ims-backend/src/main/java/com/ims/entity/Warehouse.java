package com.ims.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "warehouses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@CompoundIndexes({
    @CompoundIndex(name = "idx_tenant_location", def = "{'tenantId': 1, 'location': 1}")
})
public class Warehouse {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String name;

    private String location;

    private Integer capacity;
    private LocalDateTime createdAt;
}
