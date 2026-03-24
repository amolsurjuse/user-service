package com.electrahub.user.api.error;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
public class UnauthorizedException extends RuntimeException {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnauthorizedException.class);


    /**
     * Executes unauthorized exception for `UnauthorizedException`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api.error`.
     * @param message input consumed by UnauthorizedException.
     */
    public UnauthorizedException(String message) {
        super(message);
        LOGGER.info("CODEx_ENTRY_LOG: Entering UnauthorizedException#UnauthorizedException");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering UnauthorizedException#UnauthorizedException with debug context");
    }
}
