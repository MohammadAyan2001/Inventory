package com.ims.service.impl;

import com.ims.dto.request.LoginRequest;
import com.ims.dto.request.RegisterRequest;
import com.ims.dto.response.AuthResponse;
import com.ims.entity.User;
import com.ims.entity.Vendor;
import com.ims.enums.Role;
import com.ims.exception.DuplicateResourceException;
import com.ims.repository.UserRepository;
import com.ims.repository.VendorRepository;
import com.ims.security.JwtUtil;
import com.ims.tenant.TenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final TenantResolver tenantResolver;

    public AuthResponse register(RegisterRequest request) {
        String tenantId = resolveTenant(request.getTenantId());
        if (userRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        Role role = parseRole(request.getRole());
        String vendorId = null;

        if (role == Role.VENDOR) {
            Vendor vendor = vendorRepository.findByEmailAndTenantId(request.getEmail(), tenantId)
                .orElseGet(() -> vendorRepository.save(Vendor.builder()
                    .tenantId(tenantId)
                    .name(request.getName())
                    .email(request.getEmail())
                    .createdAt(LocalDateTime.now())
                    .build()));
            vendorId = vendor.getId();
        }

        User user = User.builder()
            .tenantId(tenantId)
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .role(role)
            .vendorId(vendorId)
            .createdAt(LocalDateTime.now())
            .build();

        userRepository.save(user);
        return buildResponse(user, jwtUtil.generateToken(user));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String tenantId = resolveTenant(request.getTenantId());
        User user = userRepository.findByEmailAndTenantId(request.getEmail(), tenantId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return buildResponse(user, jwtUtil.generateToken(user));
    }

    private Role parseRole(String roleValue) {
        try {
            return Role.valueOf(roleValue.toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid role: " + roleValue + ". Supported roles: ADMIN, WAREHOUSE_OPERATOR, VENDOR");
        }
    }

    private AuthResponse buildResponse(User user, String token) {
        return AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .email(user.getEmail())
            .role(user.getRole().name())
            .vendorId(user.getVendorId())
            .tenantId(user.getTenantId())
            .build();
    }

    private String resolveTenant(String tenantId) {
        String requestedTenant = tenantResolver.sanitize(tenantId);
        if (StringUtils.hasText(requestedTenant)) {
            return requestedTenant;
        }
        return tenantResolver.currentTenantId();
    }
}
