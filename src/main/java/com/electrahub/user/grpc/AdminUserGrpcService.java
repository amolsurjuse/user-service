package com.electrahub.user.grpc;

import com.electrahub.proto.user.v1.AdminUserServiceGrpc;
import com.electrahub.proto.user.v1.AdminSearchUsersRequest;
import com.electrahub.proto.user.v1.AdminGetUserDetailRequest;
import com.electrahub.proto.user.v1.AdminUpdateUserRequest;
import com.electrahub.proto.user.v1.AdminResetPasswordRequest;
import com.electrahub.proto.user.v1.AdminResetPasswordResponse;
import com.electrahub.proto.user.v1.AdminDeleteUserRequest;
import com.electrahub.proto.user.v1.AdminDeleteUserResponse;
import com.electrahub.proto.user.v1.AdminUserSearchResponse;
import com.electrahub.proto.user.v1.AdminUserDetailResponse;
import com.electrahub.proto.user.v1.AdminUserSummary;
import com.electrahub.user.api.dto.AddressDto;
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
    public void adminSearchUsers(
            AdminSearchUsersRequest request,
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

            responseObserver.onNext(convertToProto(searchResponse));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in adminSearchUsers", e);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asException());
        } catch (Exception e) {
            LOGGER.error("Unexpected error in adminSearchUsers", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asException());
        }
    }

    @Override
    public void adminGetUserDetail(AdminGetUserDetailRequest request, StreamObserver<AdminUserDetailResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Getting admin user: {}", request.getUserId());

            com.electrahub.user.api.dto.AdminUserDetailResponse userDetail =
                    userManagementService.getAdminUser(UUID.fromString(request.getUserId()));

            responseObserver.onNext(convertToProto(userDetail));
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            LOGGER.warn("User not found: {}", request.getUserId());
            responseObserver.onError(Status.NOT_FOUND.withDescription("User not found").asException());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in adminGetUserDetail", e);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asException());
        } catch (Exception e) {
            LOGGER.error("Unexpected error in adminGetUserDetail", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asException());
        }
    }

    @Override
    public void adminUpdateUser(
            AdminUpdateUserRequest request,
            StreamObserver<AdminUserDetailResponse> responseObserver
    ) {
        try {
            LOGGER.debug("gRPC: Updating admin user: {}", request.getUserId());

            AddressDto addressDto = toAddressDto(request.getAddress());

            com.electrahub.user.api.dto.AdminUpdateUserRequest updateRequest =
                    new com.electrahub.user.api.dto.AdminUpdateUserRequest(
                            request.getFirstName(),
                            request.getLastName(),
                            request.getPhoneNumber(),
                            request.getEnabled(),
                            addressDto
                    );

            com.electrahub.user.api.dto.AdminUserDetailResponse userDetail =
                    userManagementService.updateAdminUser(UUID.fromString(request.getUserId()), updateRequest);

            responseObserver.onNext(convertToProto(userDetail));
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            LOGGER.warn("User not found during update: {}", request.getUserId());
            responseObserver.onError(Status.NOT_FOUND.withDescription("User not found").asException());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in adminUpdateUser", e);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asException());
        } catch (Exception e) {
            LOGGER.error("Unexpected error in adminUpdateUser", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asException());
        }
    }

    @Override
    public void adminResetPassword(AdminResetPasswordRequest request, StreamObserver<AdminResetPasswordResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Resetting password for user: {}", request.getUserId());

            com.electrahub.user.api.dto.AdminResetPasswordRequest resetRequest =
                    new com.electrahub.user.api.dto.AdminResetPasswordRequest(request.getNewPassword());

            userManagementService.resetPassword(UUID.fromString(request.getUserId()), resetRequest);

            responseObserver.onNext(AdminResetPasswordResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            LOGGER.warn("User not found during password reset: {}", request.getUserId());
            responseObserver.onError(Status.NOT_FOUND.withDescription("User not found").asException());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in adminResetPassword", e);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asException());
        } catch (Exception e) {
            LOGGER.error("Unexpected error in adminResetPassword", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asException());
        }
    }

    @Override
    public void adminDeleteUser(AdminDeleteUserRequest request, StreamObserver<AdminDeleteUserResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Deleting user: {}", request.getUserId());

            userManagementService.deleteUser(UUID.fromString(request.getUserId()));

            responseObserver.onNext(AdminDeleteUserResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            LOGGER.warn("User not found during deletion: {}", request.getUserId());
            responseObserver.onError(Status.NOT_FOUND.withDescription("User not found").asException());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in adminDeleteUser", e);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asException());
        } catch (Exception e) {
            LOGGER.error("Unexpected error in adminDeleteUser", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asException());
        }
    }

    private AddressDto toAddressDto(com.electrahub.proto.common.v1.Address address) {
        if (address == null || address.getCountryIsoCode().isBlank()) {
            return null;
        }
        return new AddressDto(
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountryIsoCode()
        );
    }

    private AdminUserSearchResponse convertToProto(com.electrahub.user.api.dto.AdminUserSearchResponse searchResponse) {
        var builder = AdminUserSearchResponse.newBuilder()
                .setTotal(searchResponse.total())
                .setLimit(searchResponse.limit())
                .setOffset(searchResponse.offset())
                .setCurrentPage(searchResponse.currentPage())
                .setTotalPages(searchResponse.totalPages())
                .setHasNext(searchResponse.hasNext())
                .setHasPrevious(searchResponse.hasPrevious());

        if (searchResponse.items() != null) {
            searchResponse.items().forEach(user ->
                    builder.addItems(AdminUserSummary.newBuilder()
                            .setUserId(user.userId().toString())
                            .setEmail(user.email())
                            .setFirstName(user.firstName() != null ? user.firstName() : "")
                            .setLastName(user.lastName() != null ? user.lastName() : "")
                            .setPhoneNumber(user.phoneNumber() != null ? user.phoneNumber() : "")
                            .setEnabled(user.enabled())
                            .addAllRoles(user.roles() != null ? user.roles() : java.util.List.of())
                            .build())
            );
        }

        return builder.build();
    }

    private AdminUserDetailResponse convertToProto(com.electrahub.user.api.dto.AdminUserDetailResponse user) {
        return AdminUserDetailResponse.newBuilder()
                .setUserId(user.userId().toString())
                .setEmail(user.email())
                .setFirstName(user.firstName() != null ? user.firstName() : "")
                .setLastName(user.lastName() != null ? user.lastName() : "")
                .setPhoneNumber(user.phoneNumber() != null ? user.phoneNumber() : "")
                .setStreet(user.street() != null ? user.street() : "")
                .setCity(user.city() != null ? user.city() : "")
                .setState(user.state() != null ? user.state() : "")
                .setPostalCode(user.postalCode() != null ? user.postalCode() : "")
                .setCountryCode(user.countryCode() != null ? user.countryCode() : "")
                .setCountryName(user.countryName() != null ? user.countryName() : "")
                .setCountryDialCode(user.countryDialCode() != null ? user.countryDialCode() : "")
                .setEnabled(user.enabled())
                .addAllRoles(user.roles() != null ? user.roles() : java.util.List.of())
                .build();
    }
}
