package com.ims.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VendorRequest {
    @NotBlank @Size(max = 200)
    private String name;

    @NotBlank @Email
    private String email;

    private String phone;
    private String address;
}
