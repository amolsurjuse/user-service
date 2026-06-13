package com.electrahub.user.api;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRegistrationController.class);


    private final UserManagementService userManagementService;

    /**
     * Executes user registration controller for `UserRegistrationController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @param userManagementService input consumed by UserRegistrationController.
     */
    public UserRegistrationController(UserManagementService userManagementService) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering UserRegistrationController#UserRegistrationController");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering UserRegistrationController#UserRegistrationController with debug context");
        this.userManagementService = userManagementService;
    }

    /**
     * Creates register for `UserRegistrationController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @param request input consumed by register.
     * @return result produced by register.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserPrincipalResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return userManagementService.register(request);
    }

    /**
     * Executes authenticate for `UserRegistrationController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @param request input consumed by authenticate.
     * @return result produced by authenticate.
     */
    @PostMapping("/authenticate")
    public UserPrincipalResponse authenticate(@Valid @RequestBody AuthenticateUserRequest request) {
        return userManagementService.authenticate(request);
    }
}
