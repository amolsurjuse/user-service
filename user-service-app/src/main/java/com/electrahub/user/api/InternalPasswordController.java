package com.electrahub.user.api;

import com.electrahub.user.api.dto.ResetUserPasswordRequest;
import com.electrahub.user.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/users")
public class InternalPasswordController {
    private final UserManagementService userManagementService;

    public InternalPasswordController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping("/{userId}/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable UUID userId, @Valid @RequestBody ResetUserPasswordRequest request) {
        userManagementService.resetPassword(userId, request);
    }

    @PostMapping("/{userId}/email/verify")
    public com.electrahub.user.api.dto.UserPrincipalResponse markEmailVerified(@PathVariable UUID userId) {
        return userManagementService.markEmailVerified(userId);
    }

    @GetMapping("/{userId}/phone-verification")
    public UserManagementService.PhoneVerificationContact phoneVerification(@PathVariable UUID userId) {
        return userManagementService.getPhoneVerificationContact(userId);
    }

    @PostMapping("/{userId}/phone/verify")
    public UserManagementService.PhoneVerificationContact markPhoneVerified(@PathVariable UUID userId) {
        return userManagementService.markPhoneVerified(userId);
    }

    @GetMapping("/by-email/{email}/principal")
    public com.electrahub.user.api.dto.UserPrincipalResponse principalByEmail(@PathVariable String email) {
        return userManagementService.getPrincipalByEmail(email);
    }
}
