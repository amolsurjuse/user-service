package com.electrahub.user.security;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthFilter.class);


    private final JwtService jwtService;

    /**
     * Executes jwt auth filter for `JwtAuthFilter`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.security`.
     * @param jwtService input consumed by JwtAuthFilter.
     */
    public JwtAuthFilter(JwtService jwtService) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering JwtAuthFilter#JwtAuthFilter");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering JwtAuthFilter#JwtAuthFilter with debug context");
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            JwtService.ParsedToken parsed = jwtService.parseAndValidate(token);
            if (!jwtService.isNotExpired(parsed.exp())) {
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                AuthenticatedUser principal = new AuthenticatedUser(
                        UUID.fromString(parsed.uid()),
                        parsed.subjectEmail(),
                        parsed.roles()
                );
                var authorities = parsed.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ignored) {
            // Invalid token leaves the request unauthenticated.
        }

        filterChain.doFilter(request, response);
    }
}
