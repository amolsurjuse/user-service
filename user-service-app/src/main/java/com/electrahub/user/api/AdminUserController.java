package com.electrahub.user.api;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.user.api.dto.AdminResetPasswordRequest;
import com.electrahub.user.api.dto.AdminUpdateUserRequest;
import com.electrahub.user.api.dto.AdminUserDirectoryEntryResponse;
import com.electrahub.user.api.dto.AdminUserDetailResponse;
import com.electrahub.user.api.dto.AdminUserResolveRequest;
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

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserController.class);


    private final UserManagementService userManagementService;

    /**
     * Executes admin user controller for `AdminUserController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @param userManagementService input consumed by AdminUserController.
     */
    public AdminUserController(UserManagementService userManagementService) {
        LOGGER.info("Entering AdminUserController#AdminUserController");
        LOGGER.debug("Entering AdminUserController#AdminUserController with debug context");
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public AdminUserSearchResponse search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return userManagementService.searchAdminUsers(query, limit, offset);
    }

    @PostMapping("/resolve")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public List<AdminUserDirectoryEntryResponse> resolve(@Valid @RequestBody AdminUserResolveRequest request) {
        return userManagementService.resolveAdminUsers(request.userIds());
    }

    /**
     * Executes detail for `AdminUserController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @param userId input consumed by detail.
     * @return result produced by detail.
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public AdminUserDetailResponse detail(@PathVariable UUID userId) {
        return userManagementService.getAdminUser(userId);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public AdminUserDetailResponse update(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUpdateUserRequest request
    ) {
        return userManagementService.updateAdminUser(userId, request);
    }

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminResetPasswordRequest request
    ) {
        userManagementService.resetPassword(userId, request);
    }

    /**
     * Removes delete for `AdminUserController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @param userId input consumed by delete.
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId) {
        userManagementService.deleteUser(userId);
    }
}
