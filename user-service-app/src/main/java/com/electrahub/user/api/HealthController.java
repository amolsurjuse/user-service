package com.electrahub.user.api;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthController.class);


    /**
     * Executes ping for `HealthController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @return result produced by ping.
     */
    @GetMapping("/ping")
    public Map<String, String> ping() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering HealthController#ping");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering HealthController#ping with debug context");
        return Map.of("status", "ok");
    }
}
