package com.domuspacis.config;

import com.domuspacis.auth.domain.User;
import com.domuspacis.auth.domain.UserRole;
import com.domuspacis.auth.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.seed-password:}")
    private String adminSeedPassword;

    @Override
    @Transactional
    public void run(String... args) {
        backfillMissingPasswordChangedAt();
        if (!userRepository.existsByEmail("admin@domuspacis.com")) {
            if (!StringUtils.hasText(adminSeedPassword)) {
                log.warn("ADMIN_SEED_PASSWORD is not set. Skipping default admin user creation. "
                        + "Set the 'app.admin.seed-password' environment variable to seed an admin account.");
                return;
            }
            User admin = User.builder()
                    .email("admin@domuspacis.com")
                    .passwordHash(passwordEncoder.encode(adminSeedPassword))
                    .role(UserRole.ADMIN)
                    .firstName("Admin")
                    .lastName("Domus")
                    .isActive(true)
                    .passwordChangedAt(Instant.now())
                    .build();
            userRepository.save(admin);
            log.info("Initialized default admin user: admin@domuspacis.com");
        }
    }

    /**
     * Backfills users whose password_changed_at is null (e.g. rows with zero dates
     * converted to NULL by the MySQL connector). Without this, JWT generation
     * would fail with a NullPointerException.
     */
    private void backfillMissingPasswordChangedAt() {
        List<User> users = userRepository.findAll();
        boolean updated = false;
        for (User user : users) {
            if (user.getPasswordChangedAt() == null) {
                user.setPasswordChangedAt(Instant.now());
                userRepository.save(user);
                updated = true;
                log.info("Backfilled password_changed_at for user: {}", user.getEmail());
            }
        }
        if (updated) {
            log.info("Backfilled password_changed_at for {} user(s) with null values", users.stream()
                    .filter(u -> u.getPasswordChangedAt() != null)
                    .count());
        }
    }
}
