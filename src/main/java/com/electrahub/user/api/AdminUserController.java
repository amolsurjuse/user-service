package com.electrahub.user.api;

import com.electrahub.user.api.dto.AdminResetPasswordRequest;
import com.electrahub.user.api.dto.AdminUpdateUserRequest;
import com.electrahub.user.api.dto.AdminUserDetailResponse;
import com.electrahub.user.api.dto.AdminUserSearchResponse;
import com.electrahub.user.service.UserManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserManagementService userManagementService;

    public AdminUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public AdminUserSearchResponse search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return userManagementService.searchAdminUsers(query, limit, offset);
    }

    @GetMapping("/{userId}")
    public AdminUserDetailResponse detail(@PathVariable UUID userId) {
        return userManagementService.getAdminUser(userId);
    }

    @PutMapping("/{userId}")
    public AdminUserDetailResponse update(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUpdateUserRequest request
    ) {
        return userManagementService.updateAdminUser(userId, request);
    }

    @PostMapping("/{userId}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminResetPasswordRequest request
    ) {
        userManagementService.resetPassword(userId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId) {
        userManagementService.deleteUser(userId);
    }
}
