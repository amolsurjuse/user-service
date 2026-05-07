package com.electrahub.user.service;

import java.util.Locale;

public enum TermsAudience {
    DRIVER_PORTAL,
    ADMIN_PORTAL;

    public static TermsAudience from(String value) {
        if (value == null || value.isBlank()) {
            return DRIVER_PORTAL;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (normalized.contains("admin")) {
            return ADMIN_PORTAL;
        }
        return DRIVER_PORTAL;
    }
}
