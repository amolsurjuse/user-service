package com.electrahub.user;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UserServiceApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceApplication.class);


    /**
     * Executes main for `UserServiceApplication`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user`.
     * @param args input consumed by main.
     */
    public static void main(String[] args) {
        LOGGER.info(" Entering UserServiceApplication#main");
        LOGGER.debug(" Entering UserServiceApplication#main with debug context");
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
