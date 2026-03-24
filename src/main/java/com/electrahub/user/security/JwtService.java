package com.electrahub.user.security;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);


    private final SecretKey signingKey;
    private final String issuer;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.issuer}") String issuer
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    public record ParsedToken(String subjectEmail, String uid, Date exp, List<String> roles) {
    }

    /**
     * Executes parse and validate for `JwtService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.security`.
     * @param token input consumed by parseAndValidate.
     * @return result produced by parseAndValidate.
     */
    public ParsedToken parseAndValidate(String token) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering JwtService#parseAndValidate");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering JwtService#parseAndValidate with debug context");
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!issuer.equals(claims.getIssuer())) {
            throw new JwtException("Invalid issuer");
        }

        Object rolesObject = claims.getOrDefault("roles", List.of());
        List<String> roles = switch (rolesObject) {
            case List<?> list -> list.stream().map(String::valueOf).toList();
            case String value -> List.of(value);
            case null -> List.of();
            default -> List.of(String.valueOf(rolesObject));
        };

        return new ParsedToken(
                claims.getSubject(),
                String.valueOf(claims.get("uid")),
                claims.getExpiration(),
                roles
        );
    }

    /**
     * Executes is not expired for `JwtService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.security`.
     * @param exp input consumed by isNotExpired.
     * @return result produced by isNotExpired.
     */
    public boolean isNotExpired(Date exp) {
        return exp != null && exp.after(new Date());
    }
}
