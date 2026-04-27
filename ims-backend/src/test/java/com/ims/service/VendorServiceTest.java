package com.ims.service;

import com.ims.dto.request.VendorRequest;
import com.ims.entity.Vendor;
import com.ims.exception.DuplicateResourceException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.VendorRepository;
import com.ims.service.impl.VendorService;
import com.ims.tenant.TenantResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock private VendorRepository vendorRepository;
    @Mock private TenantResolver tenantResolver;
    @InjectMocks private VendorService vendorService;

    @Test
    void create_success() {
        VendorRequest request = new VendorRequest();
        request.setName("Acme Corp");
        request.setEmail("acme@test.com");

        when(tenantResolver.currentTenantId()).thenReturn("public");
        when(vendorRepository.existsByEmailAndTenantId("acme@test.com", "public")).thenReturn(false);
        when(vendorRepository.save(any())).thenReturn(
            Vendor.builder().id("v-1").name("Acme Corp").email("acme@test.com").build());

        Vendor result = vendorService.create(request);

        assertThat(result.getId()).isEqualTo("v-1");
        assertThat(result.getName()).isEqualTo("Acme Corp");
    }

    @Test
    void create_throwsDuplicate_whenEmailExists() {
        VendorRequest request = new VendorRequest();
        request.setEmail("existing@test.com");

        when(tenantResolver.currentTenantId()).thenReturn("public");
        when(vendorRepository.existsByEmailAndTenantId("existing@test.com", "public")).thenReturn(true);

        assertThatThrownBy(() -> vendorService.create(request))
            .isInstanceOf(DuplicateResourceException.class);

        verify(vendorRepository, never()).save(any());
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        when(tenantResolver.currentTenantId()).thenReturn("public");
        when(vendorRepository.findByIdAndTenantId("bad-id", "public")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorService.getById("bad-id"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(tenantResolver.currentTenantId()).thenReturn("public");
        when(vendorRepository.findByIdAndTenantId("bad-id", "public")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorService.delete("bad-id"))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(vendorRepository, never()).delete(any());
    }
}
