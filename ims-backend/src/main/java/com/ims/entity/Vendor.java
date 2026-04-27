package com.ims.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "vendors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@CompoundIndexes({
    @CompoundIndex(name = "uniq_tenant_vendor_email", def = "{'tenantId': 1, 'email': 1}", unique = true)
})
public class Vendor {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String name;

    @Indexed
    private String email;

    private String phone;
    private String address;
    private LocalDateTime createdAt;
}
