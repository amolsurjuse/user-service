package com.electrahub.user.security;

import com.electrahub.user.api.error.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalServiceTokenGuard {

    public static final String HEADER_NAME = "X-ElectraHub-Internal-Token";

    private final byte[] expectedToken;

    public InternalServiceTokenGuard(
            @Value("${app.security.internal-token:${APP_SECURITY_INTERNAL_TOKEN:}}") String internalToken
    ) {
        this.expectedToken = normalize(internalToken);
    }

    public void assertAuthorized(String incomingToken) {
        byte[] suppliedToken = normalize(incomingToken);
        if (expectedToken.length == 0
                || suppliedToken.length == 0
                || !MessageDigest.isEqual(expectedToken, suppliedToken)) {
            throw new UnauthorizedException("Invalid internal service token");
        }
    }

    private static byte[] normalize(String token) {
        return token == null ? new byte[0] : token.trim().getBytes(StandardCharsets.UTF_8);
    }
}
