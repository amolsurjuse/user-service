package com.electrahub.user.api;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.user.api.dto.RbacPolicyResponse;
import com.electrahub.user.api.dto.RbacPolicyUpdateRequest;
import com.electrahub.user.service.RbacPolicyService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@RequestMapping("/api/v1/admin/rbac")
public class AdminRbacController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminRbacController.class);


    private final RbacPolicyService rbacPolicyService;

    /**
     * Executes admin rbac controller for `AdminRbacController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @param rbacPolicyService input consumed by AdminRbacController.
     */
    public AdminRbacController(RbacPolicyService rbacPolicyService) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering AdminRbacController#AdminRbacController");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering AdminRbacController#AdminRbacController with debug context");
        this.rbacPolicyService = rbacPolicyService;
    }

    /**
     * Retrieves read policy for `AdminRbacController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @return result produced by readPolicy.
     */
    @GetMapping("/policy")
    public RbacPolicyResponse readPolicy() {
        return rbacPolicyService.readAdminPolicy();
    }

    /**
     * Updates update policy for `AdminRbacController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @param request input consumed by updatePolicy.
     * @return result produced by updatePolicy.
     */
    @PutMapping("/policy")
    public RbacPolicyResponse updatePolicy(@Valid @RequestBody RbacPolicyUpdateRequest request) {
        return rbacPolicyService.updatePolicy(request);
    }
}
