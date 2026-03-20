package com.electrahub.user.api;

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

    private final RbacPolicyService rbacPolicyService;

    public AdminRbacController(RbacPolicyService rbacPolicyService) {
        this.rbacPolicyService = rbacPolicyService;
    }

    @GetMapping("/policy")
    public RbacPolicyResponse readPolicy() {
        return rbacPolicyService.readAdminPolicy();
    }

    @PutMapping("/policy")
    public RbacPolicyResponse updatePolicy(@Valid @RequestBody RbacPolicyUpdateRequest request) {
        return rbacPolicyService.updatePolicy(request);
    }
}
