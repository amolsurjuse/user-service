package com.electrahub.user.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Creates an administrator whose access is defined exclusively by hierarchy
 * grants. This path intentionally does not create a driver wallet or a
 * customer role.
 */
public record AdminProvisionScopedUserRequest(
        @Email @NotBlank @Size(max = 320) String email,
        @NotBlank @Size(min = 12, max = 128) String initialPassword,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Invalid phone number") String phoneNumber,
        @NotEmpty @Size(max = 250) List<@Valid AdminScopeGrantRequest> grants,
        @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9._:-]{0,63}$", message = "Invalid tenant ID") String tenantId
) {
    public AdminProvisionScopedUserRequest(String email, String initialPassword, String firstName, String lastName,
                                           String phoneNumber, List<AdminScopeGrantRequest> grants) {
        this(email, initialPassword, firstName, lastName, phoneNumber, grants, "electrahub");
    }
}
