package com.electrahub.user.api;

import com.electrahub.user.api.dto.BillingProfileResponse;
import com.electrahub.user.service.UserManagementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/users")
public class InternalBillingProfileController {
    private final UserManagementService userManagementService;

    public InternalBillingProfileController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping("/{userId}/billing-profile")
    public BillingProfileResponse billingProfile(@PathVariable UUID userId) {
        return userManagementService.getBillingProfileInternal(userId);
    }
}
