package com.domuspacis.config;

import com.domuspacis.auth.domain.User;
import com.domuspacis.auth.domain.UserRole;
import com.domuspacis.auth.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@domuspacis.com")) {
            User admin = User.builder()
                    .email("admin@domuspacis.com")
                    .passwordHash(passwordEncoder.encode("Admindomuskigali"))
                    .role(UserRole.ADMIN)
                    .firstName("Admin")
                    .lastName("Domus")
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            log.info("Initialized default admin user: admin@domuspacis.com");
        }
    }
}
