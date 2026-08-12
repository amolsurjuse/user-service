package com.electrahub.user.api;

import com.electrahub.user.security.InternalServiceTokenGuard;
import com.electrahub.user.service.PrivacyRightsService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/privacy/requests")
public class InternalPrivacyRightsController {
    private final InternalServiceTokenGuard guard;
    private final PrivacyRightsService service;

    public InternalPrivacyRightsController(InternalServiceTokenGuard guard, PrivacyRightsService service) {
        this.guard = guard;
        this.service = service;
    }

    @PostMapping("/{requestId}/events")
    public ResponseEntity<PrivacyRightsService.RequestView> process(
            @RequestHeader(value = InternalServiceTokenGuard.HEADER_NAME, required = false) String token,
            @PathVariable UUID requestId, @RequestBody PrivacyRightsService.Process request) {
        guard.assertAuthorized(token);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.process(requestId, request));
    }
}
