package com.electrahub.user.api;

import com.electrahub.user.api.dto.AdminUserDirectoryEntryResponse;
import com.electrahub.user.api.dto.AdminUserResolveRequest;
import com.electrahub.user.api.dto.UserAudienceProfileResponse;
import com.electrahub.user.security.InternalServiceTokenGuard;
import com.electrahub.user.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal/users")
public class InternalUserDirectoryController {

    private final InternalServiceTokenGuard internalServiceTokenGuard;
    private final UserManagementService userManagementService;

    public InternalUserDirectoryController(
            InternalServiceTokenGuard internalServiceTokenGuard,
            UserManagementService userManagementService
    ) {
        this.internalServiceTokenGuard = internalServiceTokenGuard;
        this.userManagementService = userManagementService;
    }

    @PostMapping("/resolve")
    public List<AdminUserDirectoryEntryResponse> resolve(
            @RequestHeader(value = InternalServiceTokenGuard.HEADER_NAME, required = false) String internalToken,
            @Valid @RequestBody AdminUserResolveRequest request
    ) {
        internalServiceTokenGuard.assertAuthorized(internalToken);
        return userManagementService.resolveAdminUsers(request.userIds());
    }

    @GetMapping("/{userId}/audience-profile")
    public UserAudienceProfileResponse audienceProfile(
            @PathVariable UUID userId,
            @RequestHeader(value = InternalServiceTokenGuard.HEADER_NAME, required = false) String internalToken
    ) {
        internalServiceTokenGuard.assertAuthorized(internalToken);
        return userManagementService.getAudienceProfile(userId);
    }
}
