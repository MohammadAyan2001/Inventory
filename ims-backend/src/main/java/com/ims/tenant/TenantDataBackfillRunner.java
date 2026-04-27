package com.ims.tenant;

import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantDataBackfillRunner implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    @Value("${app.tenancy.default-tenant:public}")
    private String defaultTenant;

    @Value("${app.tenancy.auto-backfill:true}")
    private boolean autoBackfill;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoBackfill) {
            return;
        }

        List<String> collections = List.of("users", "vendors", "products", "inventory", "orders", "warehouses");
        for (String collection : collections) {
            backfillCollection(collection);
        }
    }

    private void backfillCollection(String collection) {
        Query query = new Query(new Criteria().orOperator(
            Criteria.where("tenantId").exists(false),
            Criteria.where("tenantId").is(null),
            Criteria.where("tenantId").is("")
        ));

        Update update = new Update().set("tenantId", defaultTenant);
        UpdateResult result = mongoTemplate.updateMulti(query, update, collection);

        if (result.getModifiedCount() > 0) {
            log.info("Tenant backfill applied for collection={} documents={}", collection, result.getModifiedCount());
        }
    }
}
