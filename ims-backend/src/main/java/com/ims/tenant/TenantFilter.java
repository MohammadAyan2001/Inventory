package com.ims.tenant;

import com.ims.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    private final JwtUtil jwtUtil;
    private final TenantResolver tenantResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String tenantId = tenantResolver.sanitize(request.getHeader(TENANT_HEADER));
        if (!StringUtils.hasText(tenantId)) {
            tenantId = extractTenantFromToken(request);
        }

        if (StringUtils.hasText(tenantId)) {
            TenantContext.setTenantId(tenantId);
        } else {
            TenantContext.clear();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractTenantFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7);
        try {
            return tenantResolver.sanitize(jwtUtil.extractTenantId(token));
        } catch (Exception ignored) {
            return null;
        }
    }
}
