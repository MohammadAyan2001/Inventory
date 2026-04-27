package com.ims.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TenantResolver {

    @Value("${app.tenancy.default-tenant:public}")
    private String defaultTenant;

    public String currentTenantId() {
        String tenantId = sanitize(TenantContext.getTenantId());
        return StringUtils.hasText(tenantId) ? tenantId : defaultTenant;
    }

    public String sanitize(String tenantId) {
        return tenantId == null ? null : tenantId.trim().toLowerCase();
    }
}
