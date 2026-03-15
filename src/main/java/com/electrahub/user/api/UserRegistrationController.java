package com.electrahub.user.api;

import com.electrahub.user.api.dto.AuthenticateUserRequest;
import com.electrahub.user.api.dto.RegisterUserRequest;
import com.electrahub.user.api.dto.UserPrincipalResponse;
import com.electrahub.user.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserRegistrationController {

    private final UserManagementService userManagementService;

    public UserRegistrationController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserPrincipalResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return userManagementService.register(request);
    }

    @PostMapping("/authenticate")
    public UserPrincipalResponse authenticate(@Valid @RequestBody AuthenticateUserRequest request) {
        return userManagementService.authenticate(request);
    }
}
