package com.electrahub.user.grpc;

import com.electrahub.proto.user.v1.RbacServiceGrpc;
import com.electrahub.proto.user.v1.GetPolicyRequest;
import com.electrahub.proto.user.v1.RbacPolicyResponse;
import com.electrahub.proto.user.v1.RbacRule;
import com.electrahub.user.api.dto.GatewayRbacPolicyResponse;
import com.electrahub.user.service.RbacPolicyService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class RbacGrpcService extends RbacServiceGrpc.RbacServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(RbacGrpcService.class);

    private final RbacPolicyService rbacPolicyService;

    public RbacGrpcService(RbacPolicyService rbacPolicyService) {
        this.rbacPolicyService = rbacPolicyService;
    }

    @Override
    public void getPolicy(
            GetPolicyRequest request,
            StreamObserver<RbacPolicyResponse> responseObserver
    ) {
        try {
            LOGGER.debug("gRPC: Reading gateway RBAC policy");
            GatewayRbacPolicyResponse policy = rbacPolicyService.readGatewayPolicy();
            responseObserver.onNext(convertToProto(policy));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in getPolicy", e);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asException());
        } catch (Exception e) {
            LOGGER.error("Unexpected error in getPolicy", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asException());
        }
    }

    private RbacPolicyResponse convertToProto(GatewayRbacPolicyResponse policy) {
        var builder = RbacPolicyResponse.newBuilder()
                .setVersion(String.valueOf(policy.version()))
                .setRoleHierarchy(policy.roleHierarchy() != null ? policy.roleHierarchy() : "")
                .setDefaultDecision(policy.defaultDecision() != null ? policy.defaultDecision() : "");

        if (policy.rules() != null) {
            policy.rules().forEach(rule ->
                    builder.addRules(RbacRule.newBuilder()
                            .setName(rule.name())
                            .addAllMethods(rule.methods() != null ? rule.methods() : java.util.List.of())
                            .setPathPattern(rule.pathPattern())
                            .setEffect(rule.effect())
                            .setAllowAnonymous(rule.allowAnonymous())
                            .addAllRequiredRoles(rule.requiredRoles() != null ? rule.requiredRoles() : java.util.List.of())
                            .build())
            );
        }

        return builder.build();
    }
}
