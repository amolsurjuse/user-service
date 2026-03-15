package com.electrahub.user.api;

import com.electrahub.user.api.dto.UserCountResponse;
import com.electrahub.user.api.dto.UserPrincipalResponse;
import com.electrahub.user.api.dto.UserProfileResponse;
import com.electrahub.user.api.dto.UserSearchResponse;
import com.electrahub.user.api.dto.UpdateUserProfileRequest;
import com.electrahub.user.service.UserManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/users")
public class UserQueryController {

    private final UserManagementService userManagementService;

    public UserQueryController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping("/{userId}/principal")
    public UserPrincipalResponse principal(@PathVariable UUID userId) {
        return userManagementService.getPrincipal(userId);
    }

    @GetMapping("/{userId}/profile")
    public UserProfileResponse profile(@PathVariable UUID userId) {
        return userManagementService.getProfile(userId);
    }

    @PutMapping("/{userId}/profile")
    public UserProfileResponse updateProfile(@PathVariable UUID userId,
                                             @Valid @RequestBody UpdateUserProfileRequest request) {
        return userManagementService.updateProfile(userId, request);
    }

    @GetMapping
    public UserSearchResponse listOrSearch(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return userManagementService.search(query, limit, offset);
    }

    @GetMapping("/search/count")
    public UserCountResponse count(@RequestParam(required = false) String query) {
        return userManagementService.count(query);
    }
}
