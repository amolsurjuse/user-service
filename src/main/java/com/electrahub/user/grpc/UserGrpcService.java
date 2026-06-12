package com.electrahub.user.grpc;

import com.electrahub.proto.user.v1.UserServiceGrpc;
import com.electrahub.proto.user.v1.GetUserRequest;
import com.electrahub.proto.user.v1.UpdateUserProfileRequest;
import com.electrahub.proto.user.v1.UserProfileResponse;
import com.electrahub.user.api.dto.UpdateUserProfileRequest;
import com.electrahub.user.api.dto.UserProfileResponse;
import com.electrahub.user.api.error.NotFoundException;
import com.electrahub.user.service.UserManagementService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@GrpcService
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserGrpcService.class);

    private final UserManagementService userManagementService;

    public UserGrpcService(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @Override
    public void getProfile(GetUserRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Getting profile for user: {}", request.getUserId());

            com.electrahub.user.api.dto.UserProfileResponse profile = userManagementService.getUserProfile(
                    UUID.fromString(request.getUserId())
            );

            UserProfileResponse response = convertToProto(profile);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            LOGGER.warn("User not found: {}", request.getUserId());
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("User not found")
                            .asException()
            );
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in getProfile", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in getProfile", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void updateProfile(
            UpdateUserProfileRequest request,
            StreamObserver<UserProfileResponse> responseObserver
    ) {
        try {
            LOGGER.debug("gRPC: Updating profile for user: {}", request.getUserId());

            com.electrahub.user.api.dto.UpdateUserProfileRequest updateRequest =
                    new com.electrahub.user.api.dto.UpdateUserProfileRequest(
                            request.getFirstName(),
                            request.getLastName(),
                            request.getPhoneNumber()
                    );

            com.electrahub.user.api.dto.UserProfileResponse profile = userManagementService.updateUserProfile(
                    UUID.fromString(request.getUserId()),
                    updateRequest
            );

            UserProfileResponse response = convertToProto(profile);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            LOGGER.warn("User not found during update: {}", request.getUserId());
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("User not found")
                            .asException()
            );
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in updateProfile", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in updateProfile", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    private UserProfileResponse convertToProto(com.electrahub.user.api.dto.UserProfileResponse profile) {
        return UserProfileResponse.newBuilder()
                .setUserId(profile.userId().toString())
                .setEmail(profile.email())
                .setFirstName(profile.firstName() != null ? profile.firstName() : "")
                .setLastName(profile.lastName() != null ? profile.lastName() : "")
                .setPhoneNumber(profile.phoneNumber() != null ? profile.phoneNumber() : "")
                .setEnabled(profile.enabled())
                .build();
    }
}
