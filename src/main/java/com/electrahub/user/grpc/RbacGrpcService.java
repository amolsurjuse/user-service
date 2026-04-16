package com.electrahub.user.grpc;

import com.electrahub.proto.user.v1.RbacServiceGrpc;
import com.electrahub.proto.user.v1.ReadGatewayPolicyRequest;
import com.electrahub.proto.user.v1.RbacPolicyResponse;
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
    public void readGatewayPolicy(
            ReadGatewayPolicyRequest request,
            StreamObserver<RbacPolicyResponse> responseObserver
    ) {
        try {
            LOGGER.debug("gRPC: Reading gateway RBAC policy");
            GatewayRbacPolicyResponse policy = rbacPolicyService.readGatewayPolicy();
            RbacPolicyResponse response = convertToProto(policy);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in readGatewayPolicy", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in readGatewayPolicy", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    private RbacPolicyResponse convertToProto(GatewayRbacPolicyResponse policy) {
        return RbacPolicyResponse.newBuilder()
                .setVersion(policy.version() != null ? policy.version() : "")
                .putAllPolicies(policy.policies() != null ? policy.policies() : java.util.Map.of())
                .build();
    }
}
