package com.ims.security;

import com.ims.repository.UserRepository;
import com.ims.tenant.TenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TenantResolver tenantResolver;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String tenantId = tenantResolver.currentTenantId();
        return userRepository.findByEmailAndTenantId(email, tenantId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
