package com.electrahub.user.api;

import com.electrahub.user.api.dto.CountryResponse;
import com.electrahub.user.service.UserManagementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/countries")
public class CountryController {

    private final UserManagementService userManagementService;

    public CountryController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public List<CountryResponse> listCountries() {
        return userManagementService.countries();
    }
}
