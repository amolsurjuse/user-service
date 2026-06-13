package com.electrahub.user.api;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.user.api.dto.GatewayRbacPolicyResponse;
import com.electrahub.user.security.InternalApiKeyGuard;
import com.electrahub.user.service.RbacPolicyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/rbac")
public class InternalRbacController {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalRbacController.class);


    private final InternalApiKeyGuard internalApiKeyGuard;
    private final RbacPolicyService rbacPolicyService;

    /**
     * Executes internal rbac controller for `InternalRbacController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @param internalApiKeyGuard input consumed by InternalRbacController.
     * @param rbacPolicyService input consumed by InternalRbacController.
     */
    public InternalRbacController(InternalApiKeyGuard internalApiKeyGuard, RbacPolicyService rbacPolicyService) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering InternalRbacController#InternalRbacController");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering InternalRbacController#InternalRbacController with debug context");
        this.internalApiKeyGuard = internalApiKeyGuard;
        this.rbacPolicyService = rbacPolicyService;
    }

    @GetMapping("/policy")
    public GatewayRbacPolicyResponse readPolicy(
            @RequestHeader(value = InternalApiKeyGuard.HEADER_NAME, required = false) String internalApiKey
    ) {
        internalApiKeyGuard.assertAuthorized(internalApiKey);
        return rbacPolicyService.readGatewayPolicy();
    }
}
