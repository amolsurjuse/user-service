package com.electrahub.user.grpc;

import com.electrahub.proto.user.v1.AdminRbacServiceGrpc;
import com.electrahub.proto.user.v1.ReadAdminRbacPolicyRequest;
import com.electrahub.proto.user.v1.UpdateAdminRbacPolicyRequest;
import com.electrahub.proto.user.v1.RbacPolicyResponse;
import com.electrahub.user.api.dto.RbacPolicyResponse;
import com.electrahub.user.api.dto.RbacPolicyUpdateRequest;
import com.electrahub.user.api.dto.RbacRuleRequest;
import com.electrahub.user.service.RbacPolicyService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@GrpcService
public class AdminRbacGrpcService extends AdminRbacServiceGrpc.AdminRbacServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminRbacGrpcService.class);

    private final RbacPolicyService rbacPolicyService;

    public AdminRbacGrpcService(RbacPolicyService rbacPolicyService) {
        this.rbacPolicyService = rbacPolicyService;
    }

    @Override
    public void readAdminPolicy(
            ReadAdminRbacPolicyRequest request,
            StreamObserver<RbacPolicyResponse> responseObserver
    ) {
        try {
            LOGGER.debug("gRPC: Reading admin RBAC policy");

            RbacPolicyResponse policy = rbacPolicyService.readAdminPolicy();
            com.electrahub.proto.user.v1.RbacPolicyResponse response = convertToProto(policy);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalStateException e) {
            LOGGER.warn("Policy not configured", e);
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("RBAC policy not configured")
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in readAdminPolicy", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void updateAdminPolicy(
            UpdateAdminRbacPolicyRequest request,
            StreamObserver<RbacPolicyResponse> responseObserver
    ) {
        try {
            LOGGER.debug("gRPC: Updating admin RBAC policy");

            List<RbacRuleRequest> rules = request.getRulesList().stream()
                    .map(rule -> new RbacRuleRequest(
                            rule.getName(),
                            rule.getPathPattern(),
                            rule.getEffect(),
                            rule.getMethodsList(),
                            rule.getAllowAnonymous(),
                            rule.getRequiredRolesList()
                    ))
                    .collect(Collectors.toList());

            RbacPolicyUpdateRequest updateRequest = new RbacPolicyUpdateRequest(
                    request.getRoleHierarchy(),
                    request.getDefaultDecision(),
                    rules
            );

            RbacPolicyResponse policy = rbacPolicyService.updatePolicy(updateRequest);
            com.electrahub.proto.user.v1.RbacPolicyResponse response = convertToProto(policy);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in updateAdminPolicy", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (IllegalStateException e) {
            LOGGER.warn("Invalid state in updateAdminPolicy", e);
            responseObserver.onError(
                    Status.FAILED_PRECONDITION
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in updateAdminPolicy", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    private com.electrahub.proto.user.v1.RbacPolicyResponse convertToProto(RbacPolicyResponse policy) {
        var builder = com.electrahub.proto.user.v1.RbacPolicyResponse.newBuilder()
                .setVersion(policy.version() != null ? policy.version().toString() : "")
                .setRoleHierarchy(policy.roleHierarchy() != null ? policy.roleHierarchy() : "")
                .setDefaultDecision(policy.defaultDecision() != null ? policy.defaultDecision() : "");

        if (policy.availableRoles() != null) {
            policy.availableRoles().forEach(builder::addAvailableRoles);
        }

        if (policy.rules() != null) {
            policy.rules().forEach(rule ->
                    builder.addRules(com.electrahub.proto.user.v1.RbacRule.newBuilder()
                            .setId(rule.id().toString())
                            .setSortOrder(rule.sortOrder())
                            .setName(rule.name())
                            .setPathPattern(rule.pathPattern())
                            .setEffect(rule.effect())
                            .setAllowAnonymous(rule.allowAnonymous())
                            .addAllMethods(rule.methods() != null ? rule.methods() : List.of())
                            .addAllRequiredRoles(rule.requiredRoles() != null ? rule.requiredRoles() : List.of())
                            .build())
            );
        }

        return builder.build();
    }
}
