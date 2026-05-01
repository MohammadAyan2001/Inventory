package com.ims.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for cross-origin requests from the Angular frontend.
 *
 * Set CORS_ALLOWED_ORIGINS on Render to your exact Vercel URL:
 *   https://your-app.vercel.app,http://localhost:4200
 *
 * Pattern wildcards are supported, e.g.:
 *   https://*.vercel.app,http://localhost:4200
 *
 * Key rules:
 * - addAllowedOriginPattern() supports wildcards AND works with allowCredentials=true
 * - setAllowedOrigins() with "*" is INCOMPATIBLE with allowCredentials=true
 * - The filter must be registered on "/**" not just "/api/**" so that
 *   Spring Security's CORS processing (via .cors(withDefaults())) finds it
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String allowedOriginsRaw;

    @Bean
    public CorsFilter corsFilter() {
        List<String> patterns = Arrays.stream(allowedOriginsRaw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();

        CorsConfiguration config = new CorsConfiguration();

        // addAllowedOriginPattern supports:
        //   - exact origins:  https://my-app.vercel.app
        //   - wildcards:      https://*.vercel.app
        //   - all origins:    *
        // AND is compatible with allowCredentials=true (unlike setAllowedOrigins("*"))
        patterns.forEach(config::addAllowedOriginPattern);

        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Tenant-ID"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Register on /** so Spring Security's .cors(withDefaults()) can find
        // this configuration for ALL paths including preflight OPTIONS requests
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
