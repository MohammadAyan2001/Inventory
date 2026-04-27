package com.ims.entity;

import com.ims.enums.OrderStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Document(collection = "orders")
@CompoundIndexes({
    @CompoundIndex(name = "uniq_tenant_order_reference", def = "{'tenantId': 1, 'orderReference': 1}", unique = true),
    @CompoundIndex(name = "uniq_tenant_idempotency", def = "{'tenantId': 1, 'idempotencyKey': 1}", unique = true),
    @CompoundIndex(name = "idx_tenant_vendor_status", def = "{'tenantId': 1, 'vendorId': 1, 'status': 1}"),
    @CompoundIndex(name = "idx_tenant_created_by", def = "{'tenantId': 1, 'createdByUserId': 1, 'createdAt': -1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String orderReference;

    @Indexed
    private String idempotencyKey;

    @Indexed
    private String createdByUserId;

    private String createdByEmail;

    @Indexed
    private String vendorId;

    private String vendorName;

    private OrderStatus status;
    private BigDecimal totalAmount;

    private List<OrderItem> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static String generateReference() {
        return "PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
