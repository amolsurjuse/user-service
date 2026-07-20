package com.electrahub.user.api;

import com.electrahub.user.api.dto.AdminAccessContextResponse;
import com.electrahub.user.api.dto.AdminProvisionScopedUserRequest;
import com.electrahub.user.api.dto.AdminProvisionScopedUserResponse;
import com.electrahub.user.api.dto.AdminScopeGrantRequest;
import com.electrahub.user.api.dto.AdminScopeGrantResponse;
import com.electrahub.user.service.AdminScopeGrantService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/access")
public class AdminAccessController {

    private final AdminScopeGrantService adminScopeGrantService;

    public AdminAccessController(AdminScopeGrantService adminScopeGrantService) {
        this.adminScopeGrantService = adminScopeGrantService;
    }

    @GetMapping("/me")
    public AdminAccessContextResponse me() {
        return adminScopeGrantService.currentAccessContext();
    }

    @GetMapping("/users/{userId}/grants")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public List<AdminScopeGrantResponse> listUserGrants(@PathVariable UUID userId) {
        return adminScopeGrantService.listGrants(userId);
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProvisionScopedUserResponse provisionScopedUser(
            @Valid @RequestBody AdminProvisionScopedUserRequest request
    ) {
        return adminScopeGrantService.provisionScopedUser(request);
    }

    @PutMapping("/users/{userId}/grants")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public List<AdminScopeGrantResponse> replaceUserGrants(
            @PathVariable UUID userId,
            @Valid @RequestBody List<@Valid AdminScopeGrantRequest> grants
    ) {
        return adminScopeGrantService.replaceGrants(userId, grants);
    }
}
