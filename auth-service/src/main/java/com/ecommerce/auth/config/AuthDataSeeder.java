package com.ecommerce.auth.config;

import com.ecommerce.auth.domain.Role;
import com.ecommerce.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AuthDataSeeder implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthDataSeeder.class);

    private final AuthProperties properties;
    private final UserService userService;

    public AuthDataSeeder(AuthProperties properties, UserService userService) {
        this.properties = properties;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.seed().enabled()) {
            return;
        }

        seed(properties.seed().admin(), Role.ADMIN);
        seed(properties.seed().user(), Role.USER);
    }

    private void seed(AuthProperties.SeedAccount account, Role role) {
        boolean created = userService.createSeedUserIfAbsent(
                account.email(),
                account.displayName(),
                account.password(),
                role
        );
        if (created) {
            LOGGER.info("Created POC seed account email={} role={}", account.email(), role);
        }
    }
}
