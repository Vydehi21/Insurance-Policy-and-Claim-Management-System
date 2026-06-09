package com.monocept.project.config;

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

            String adminEmail = "admin@gmail.com";

            if (!userRepository.existsByEmail(adminEmail)) {

                User admin = new User();

                admin.setFullName("System Admin");
                admin.setEmail(adminEmail);

                admin.setPassword(
                    passwordEncoder.encode("admin123")
                );
                admin.setMobileNumber("1234567890");

                admin.setRole(Role.ADMIN);

                userRepository.save(admin);

                System.out.println("Default admin created");
            }
            else {
                System.out.println("Admin already exists");
            }
        };
    }
}