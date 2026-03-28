package com.electrahub.user.grpc;

import com.electrahub.proto.user.v1.AdminUserServiceGrpc;
import com.electrahub.proto.user.v1.SearchAdminUsersRequest;
import com.electrahub.proto.user.v1.GetAdminUserRequest;
import com.electrahub.proto.user.v1.UpdateAdminUserRequest;
import com.electrahub.proto.user.v1.ResetPasswordRequest;
import com.electrahub.proto.user.v1.DeleteUserRequest;
import com.electrahub.proto.user.v1.AdminUserSearchResponse;
import com.electrahub.proto.user.v1.AdminUserDetailResponse;
import com.electrahub.user.api.dto.AdminUserDetailResponse;
import com.electrahub.user.api.dto.AdminUserSearchResponse;
import com.electrahub.user.api.dto.AdminUpdateUserRequest;
import com.electrahub.user.api.dto.AdminResetPasswordRequest;
import com.electrahub.user.api.error.NotFoundException;
import com.electrahub.user.service.UserManagementService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@GrpcService
public class AdminUserGrpcService extends AdminUserServiceGrpc.AdminUserServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserGrpcService.class);

    private final UserManagementService userManagementService;

    public AdminUserGrpcService(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @Override
    public void searchAdminUsers(
            SearchAdminUsersRequest request,
            StreamObserver<AdminUserSearchResponse> responseObserver
    ) {
        try {
            LOGGER.debug("gRPC: Searching admin users with query: {}, limit: {}, offset: {}",
                    request.getQuery(), request.getLimit(), request.getOffset());

            com.electrahub.user.api.dto.AdminUserSearchResponse searchResponse =
                    userManagementService.searchAdminUsers(
                            request.getQuery().isEmpty() ? null : request.getQuery(),
                            request.getLimit(),
                            request.getOffset()
                    );

            AdminUserSearchResponse response = convertToProto(searchResponse);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in searchAdminUsers", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in searchAdminUsers", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void getAdminUser(GetAdminUserRequest request, StreamObserver<AdminUserDetailResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Getting admin user: {}", request.getUserId());

            com.electrahub.user.api.dto.AdminUserDetailResponse userDetail =
                    userManagementService.getAdminUser(UUID.fromString(request.getUserId()));

            AdminUserDetailResponse response = convertToProto(userDetail);
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
            LOGGER.warn("Invalid argument in getAdminUser", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in getAdminUser", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void updateAdminUser(
            UpdateAdminUserRequest request,
            StreamObserver<AdminUserDetailResponse> responseObserver
    ) {
        try {
            LOGGER.debug("gRPC: Updating admin user: {}", request.getUserId());

            AdminUpdateUserRequest updateRequest = new AdminUpdateUserRequest(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPhoneNumber(),
                    request.getEnabled()
            );

            com.electrahub.user.api.dto.AdminUserDetailResponse userDetail =
                    userManagementService.updateAdminUser(UUID.fromString(request.getUserId()), updateRequest);

            AdminUserDetailResponse response = convertToProto(userDetail);
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
            LOGGER.warn("Invalid argument in updateAdminUser", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in updateAdminUser", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            LOGGER.debug("gRPC: Resetting password for user: {}", request.getUserId());

            AdminResetPasswordRequest resetRequest = new AdminResetPasswordRequest(request.getNewPassword());

            userManagementService.resetPassword(UUID.fromString(request.getUserId()), resetRequest);

            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            LOGGER.warn("User not found during password reset: {}", request.getUserId());
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("User not found")
                            .asException()
            );
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in resetPassword", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in resetPassword", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void deleteUser(DeleteUserRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            LOGGER.debug("gRPC: Deleting user: {}", request.getUserId());

            userManagementService.deleteUser(UUID.fromString(request.getUserId()));

            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            LOGGER.warn("User not found during deletion: {}", request.getUserId());
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("User not found")
                            .asException()
            );
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in deleteUser", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in deleteUser", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    private AdminUserSearchResponse convertToProto(com.electrahub.user.api.dto.AdminUserSearchResponse searchResponse) {
        var builder = AdminUserSearchResponse.newBuilder();
        if (searchResponse.users() != null) {
            searchResponse.users().forEach(user ->
                    builder.addUsers(com.electrahub.proto.user.v1.AdminUserDetailResponse.newBuilder()
                            .setUserId(user.userId().toString())
                            .setEmail(user.email())
                            .setFirstName(user.firstName() != null ? user.firstName() : "")
                            .setLastName(user.lastName() != null ? user.lastName() : "")
                            .setPhoneNumber(user.phoneNumber() != null ? user.phoneNumber() : "")
                            .setEnabled(user.enabled())
                            .build())
            );
        }
        builder.setTotal(searchResponse.total());
        return builder.build();
    }

    private AdminUserDetailResponse convertToProto(com.electrahub.user.api.dto.AdminUserDetailResponse user) {
        return AdminUserDetailResponse.newBuilder()
                .setUserId(user.userId().toString())
                .setEmail(user.email())
                .setFirstName(user.firstName() != null ? user.firstName() : "")
                .setLastName(user.lastName() != null ? user.lastName() : "")
                .setPhoneNumber(user.phoneNumber() != null ? user.phoneNumber() : "")
                .setEnabled(user.enabled())
                .build();
    }
}
