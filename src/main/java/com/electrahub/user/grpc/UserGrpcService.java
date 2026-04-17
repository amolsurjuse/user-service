package com.electrahub.user.grpc;

import com.electrahub.proto.user.v1.UserServiceGrpc;
import com.electrahub.proto.user.v1.GetUserProfileRequest;
import com.electrahub.proto.user.v1.UpdateUserProfileRequest;
import com.electrahub.proto.user.v1.UserProfileResponse;
import com.electrahub.user.api.dto.AddressDto;
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
    public void getUserProfile(GetUserProfileRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Getting profile for user: {}", request.getUserId());

            com.electrahub.user.api.dto.UserProfileResponse profile = userManagementService.getProfile(
                    UUID.fromString(request.getUserId())
            );

            responseObserver.onNext(convertToProto(profile));
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            LOGGER.warn("User not found: {}", request.getUserId());
            responseObserver.onError(Status.NOT_FOUND.withDescription("User not found").asException());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in getUserProfile", e);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asException());
        } catch (Exception e) {
            LOGGER.error("Unexpected error in getUserProfile", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asException());
        }
    }

    @Override
    public void updateUserProfile(
            UpdateUserProfileRequest request,
            StreamObserver<UserProfileResponse> responseObserver
    ) {
        try {
            LOGGER.debug("gRPC: Updating profile for user: {}", request.getUserId());

            AddressDto addressDto = toAddressDto(request.getAddress());

            com.electrahub.user.api.dto.UpdateUserProfileRequest updateRequest =
                    new com.electrahub.user.api.dto.UpdateUserProfileRequest(
                            request.getFirstName(),
                            request.getLastName(),
                            addressDto
                    );

            com.electrahub.user.api.dto.UserProfileResponse profile = userManagementService.updateProfile(
                    UUID.fromString(request.getUserId()),
                    updateRequest
            );

            responseObserver.onNext(convertToProto(profile));
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            LOGGER.warn("User not found during update: {}", request.getUserId());
            responseObserver.onError(Status.NOT_FOUND.withDescription("User not found").asException());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in updateUserProfile", e);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asException());
        } catch (Exception e) {
            LOGGER.error("Unexpected error in updateUserProfile", e);
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

    private UserProfileResponse convertToProto(com.electrahub.user.api.dto.UserProfileResponse profile) {
        return UserProfileResponse.newBuilder()
                .setUserId(profile.userId().toString())
                .setEmail(profile.email())
                .setFirstName(profile.firstName() != null ? profile.firstName() : "")
                .setLastName(profile.lastName() != null ? profile.lastName() : "")
                .setPhoneNumber(profile.phoneNumber() != null ? profile.phoneNumber() : "")
                .setStreet(profile.street() != null ? profile.street() : "")
                .setCity(profile.city() != null ? profile.city() : "")
                .setState(profile.state() != null ? profile.state() : "")
                .setPostalCode(profile.postalCode() != null ? profile.postalCode() : "")
                .setCountryCode(profile.countryCode() != null ? profile.countryCode() : "")
                .setCountryName(profile.countryName() != null ? profile.countryName() : "")
                .setCountryDialCode(profile.countryDialCode() != null ? profile.countryDialCode() : "")
                .setEnabled(profile.enabled())
                .build();
    }
}
