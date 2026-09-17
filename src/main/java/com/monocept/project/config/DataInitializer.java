package com.monocept.project.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.monocept.project.enums.Role;
import com.monocept.project.model.User;
import com.monocept.project.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {

        return args -> {

            // Only used when the database has no admin yet. The password meets the
            // app's own password policy (the old "admin123" did not).
            String adminEmail = "admin@gmail.com";

            if (!userRepository.existsByEmail(adminEmail)) {

                User admin = new User();

                admin.setFullName("System Admin");

                admin.setEmail(adminEmail);

                
                admin.setPassword(
                    passwordEncoder.encode("Admin@123")
                );
                admin.setMobileNumber("+919999999999");

                admin.setRole(Role.ADMIN);

                admin.setActiveStatus(true);

                admin.setCreatedDate(LocalDateTime.now());
                admin.setUpdatedDate(LocalDateTime.now());

                userRepository.save(admin);

                System.out.println("Default admin created");
            }
        };
    }
}