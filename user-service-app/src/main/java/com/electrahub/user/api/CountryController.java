package com.electrahub.user.api;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.user.api.dto.CountryResponse;
import com.electrahub.user.service.UserManagementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/countries")
public class CountryController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryController.class);


    private final UserManagementService userManagementService;

    /**
     * Executes country controller for `CountryController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @param userManagementService input consumed by CountryController.
     */
    public CountryController(UserManagementService userManagementService) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering CountryController#CountryController");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering CountryController#CountryController with debug context");
        this.userManagementService = userManagementService;
    }

    /**
     * Retrieves list countries for `CountryController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.api`.
     * @return result produced by listCountries.
     */
    @GetMapping
    public List<CountryResponse> listCountries() {
        return userManagementService.countries();
    }
}
