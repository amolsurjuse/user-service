package com.electrahub.user.api;

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

    private final InternalApiKeyGuard internalApiKeyGuard;
    private final RbacPolicyService rbacPolicyService;

    public InternalRbacController(InternalApiKeyGuard internalApiKeyGuard, RbacPolicyService rbacPolicyService) {
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
