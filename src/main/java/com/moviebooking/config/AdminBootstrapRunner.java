package com.moviebooking.config;

import com.moviebooking.entity.Role;
import com.moviebooking.entity.User;
import com.moviebooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Public registration (see AuthService#register) can only ever create CUSTOMER
 * accounts by design, so there needs to be some out-of-band way to get the first
 * ADMIN account into the system. This seeds exactly one on startup, controlled via
 * env vars, and is a no-op if an admin already exists or is disabled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap-enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.admin.bootstrap-email:admin@moviebooking.local}")
    private String adminEmail;

    @Value("${app.admin.bootstrap-password:Admin@12345}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!bootstrapEnabled) {
            return;
        }
        boolean anyAdminExists = userRepository.findByEmail(adminEmail).isPresent();
        if (anyAdminExists) {
            return;
        }
        User admin = User.builder()
                .name("System Admin")
                .email(adminEmail.toLowerCase())
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);
        log.info("Seeded default admin account: {} (change the password via APP_ADMIN_BOOTSTRAP_PASSWORD in real deployments)", adminEmail);
    }
}
