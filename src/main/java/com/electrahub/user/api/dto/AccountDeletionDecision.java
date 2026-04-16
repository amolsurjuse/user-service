package com.electrahub.user.api.dto;

public enum AccountDeletionDecision {
    ACTIVE_SESSION_IN_PROGRESS,
    CONFIRM_DIRECT_DELETION,
    ACCOUNT_MARKED_PENDING_DELETION,
    ACCOUNT_DELETED,
    ALREADY_PENDING_DELETION
}

