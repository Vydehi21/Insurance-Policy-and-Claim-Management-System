package com.monocept.project.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/auth/**")
                        .permitAll()

                        .requestMatchers(
                                "/error")
                        .permitAll()

                        .requestMatchers(
                                "/api/products/**")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/plans/**")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/users/**")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/customers/**")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/policies/purchase")
                        .hasRole("CUSTOMER")

                        .requestMatchers(
                                "/api/premium-payments/**")
                        .hasRole("CUSTOMER")

                        .requestMatchers(
                                "/api/claims/*/review")
                        .hasRole("AGENT")

                        .requestMatchers(
                                "/api/claims/*/decision")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/claim-history/**")
                        .hasAnyRole("ADMIN", "AGENT")

                        .anyRequest()
                        .authenticated())

                .httpBasic(Customizer.withDefaults())

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }
}