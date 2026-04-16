package com.electrahub.user.api;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.user.api.dto.AccountDeletionRequest;
import com.electrahub.user.api.dto.AccountDeletionResponse;
import com.electrahub.user.api.dto.UserCountResponse;
import com.electrahub.user.api.dto.UserPrincipalResponse;
import com.electrahub.user.api.dto.UserProfileResponse;
import com.electrahub.user.api.dto.UserSearchResponse;
import com.electrahub.user.api.dto.UpdateUserProfileRequest;
import com.electrahub.user.service.UserManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.PostMapping;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(UserQueryController.class);


    private final UserManagementService userManagementService;

    /**
     * Executes user query controller for `UserQueryController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @param userManagementService input consumed by UserQueryController.
     */
    public UserQueryController(UserManagementService userManagementService) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering UserQueryController#UserQueryController");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering UserQueryController#UserQueryController with debug context");
        this.userManagementService = userManagementService;
    }

    /**
     * Executes principal for `UserQueryController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @param userId input consumed by principal.
     * @return result produced by principal.
     */
    @GetMapping("/{userId}/principal")
    public UserPrincipalResponse principal(@PathVariable UUID userId) {
        return userManagementService.getPrincipal(userId);
    }

    /**
     * Executes profile for `UserQueryController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @param userId input consumed by profile.
     * @return result produced by profile.
     */
    @GetMapping("/{userId}/profile")
    public UserProfileResponse profile(@PathVariable UUID userId) {
        return userManagementService.getProfile(userId);
    }

    @PutMapping("/{userId}/profile")
    public UserProfileResponse updateProfile(@PathVariable UUID userId,
                                             @Valid @RequestBody UpdateUserProfileRequest request) {
        return userManagementService.updateProfile(userId, request);
    }

    @PostMapping("/{userId}/account-deletion")
    public AccountDeletionResponse requestAccountDeletion(@PathVariable UUID userId,
                                                          @RequestBody(required = false) AccountDeletionRequest request) {
        boolean confirmDirectDeletion = request != null && request.confirmDirectDeletion();
        return userManagementService.requestAccountDeletion(userId, confirmDirectDeletion);
    }

    @GetMapping
    public UserSearchResponse listOrSearch(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return userManagementService.search(query, limit, offset);
    }

    /**
     * Executes request param for `UserQueryController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @param query input consumed by RequestParam.
     * @return result produced by RequestParam.
     */
    @GetMapping("/search/count")
    public UserCountResponse count(@RequestParam(required = false) String query) {
        return userManagementService.count(query);
    }
}
