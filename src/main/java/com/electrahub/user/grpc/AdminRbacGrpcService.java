package com.electrahub.user.grpc;

import com.electrahub.proto.user.v1.AdminRbacServiceGrpc;
import com.electrahub.proto.user.v1.GetAdminPolicyRequest;
import com.electrahub.proto.user.v1.UpdateAdminPolicyRequest;
import com.electrahub.proto.user.v1.AdminRbacPolicyResponse;
import com.electrahub.proto.user.v1.AdminRbacRule;
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
    public void getAdminPolicy(
            GetAdminPolicyRequest request,
            StreamObserver<AdminRbacPolicyResponse> responseObserver
    ) {
        try {
            LOGGER.debug("gRPC: Reading admin RBAC policy");

            RbacPolicyResponse policy = rbacPolicyService.readAdminPolicy();
            responseObserver.onNext(convertToProto(policy));
            responseObserver.onCompleted();
        } catch (IllegalStateException e) {
            LOGGER.warn("Policy not configured", e);
            responseObserver.onError(Status.NOT_FOUND.withDescription("RBAC policy not configured").asException());
        } catch (Exception e) {
            LOGGER.error("Unexpected error in getAdminPolicy", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asException());
        }
    }

    @Override
    public void updateAdminPolicy(
            UpdateAdminPolicyRequest request,
            StreamObserver<AdminRbacPolicyResponse> responseObserver
    ) {
        try {
            LOGGER.debug("gRPC: Updating admin RBAC policy");

            List<RbacRuleRequest> rules = request.getRulesList().stream()
                    .map(rule -> new RbacRuleRequest(
                            rule.getName(),
                            rule.getMethodsList(),
                            rule.getPathPattern(),
                            rule.getEffect(),
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
            responseObserver.onNext(convertToProto(policy));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in updateAdminPolicy", e);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asException());
        } catch (IllegalStateException e) {
            LOGGER.warn("Invalid state in updateAdminPolicy", e);
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asException());
        } catch (Exception e) {
            LOGGER.error("Unexpected error in updateAdminPolicy", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asException());
        }
    }

    private AdminRbacPolicyResponse convertToProto(RbacPolicyResponse policy) {
        var builder = AdminRbacPolicyResponse.newBuilder()
                .setPolicyKey(policy.policyKey() != null ? policy.policyKey() : "")
                .setRoleHierarchy(policy.roleHierarchy() != null ? policy.roleHierarchy() : "")
                .setDefaultDecision(policy.defaultDecision() != null ? policy.defaultDecision() : "")
                .setVersion(policy.version());

        if (policy.availableRoles() != null) {
            policy.availableRoles().forEach(builder::addAvailableRoles);
        }

        if (policy.updatedAt() != null) {
            builder.setUpdatedAt(com.google.protobuf.Timestamp.newBuilder()
                    .setSeconds(policy.updatedAt().toEpochSecond())
                    .setNanos(policy.updatedAt().getNano())
                    .build());
        }

        if (policy.rules() != null) {
            policy.rules().forEach(rule ->
                    builder.addRules(AdminRbacRule.newBuilder()
                            .setRuleId(rule.ruleId().toString())
                            .setSortOrder(rule.sortOrder())
                            .setName(rule.name())
                            .addAllMethods(rule.methods() != null ? rule.methods() : List.of())
                            .setPathPattern(rule.pathPattern())
                            .setEffect(rule.effect())
                            .setAllowAnonymous(rule.allowAnonymous())
                            .addAllRequiredRoles(rule.requiredRoles() != null ? rule.requiredRoles() : List.of())
                            .build())
            );
        }

        return builder.build();
    }
}
