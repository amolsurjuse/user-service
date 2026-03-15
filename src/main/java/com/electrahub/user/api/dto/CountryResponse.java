package com.electrahub.user.api.dto;

public record CountryResponse(
        String code,
        String name,
        String dialCode
) {
}
