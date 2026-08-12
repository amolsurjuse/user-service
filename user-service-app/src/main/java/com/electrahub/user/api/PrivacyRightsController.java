package com.electrahub.user.api;

import com.electrahub.user.security.AuthenticatedUser;
import com.electrahub.user.service.PrivacyRightsService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/privacy")
public class PrivacyRightsController {
    private final PrivacyRightsService service;
    public PrivacyRightsController(PrivacyRightsService service) { this.service = service; }

    @GetMapping("/export")
    public ResponseEntity<PrivacyRightsService.Export> export(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.export(require(user).userId()));
    }

    @PostMapping("/requests")
    public ResponseEntity<PrivacyRightsService.RequestView> submit(@AuthenticationPrincipal AuthenticatedUser user,
                                                                   @RequestBody PrivacyRightsService.Submit request) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(service.submit(require(user).userId(), request));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<PrivacyRightsService.RequestView>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(require(user).userId()));
    }

    private static AuthenticatedUser require(AuthenticatedUser user) {
        if (user == null) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        return user;
    }
}
