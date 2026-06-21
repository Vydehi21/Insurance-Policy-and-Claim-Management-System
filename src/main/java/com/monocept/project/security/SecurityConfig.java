package com.monocept.project.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
                
                .cors(cors->cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // PUBLIC ROUTES
                        .requestMatchers("/api/files/upload",
                        		"/api/claims",
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // USER MANAGEMENT
                        // ADMIN ONLY                    
                        .requestMatchers("/api/users/**")
                        .hasRole("ADMIN")

                        // INSURANCE PRODUCTS
                        
                        // Everyone logged in can view products
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "AGENT",
                                "CUSTOMER"
                        )

                        // Only admin can create/update/deactivate
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/products/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/products/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/products/**"
                        )
                        .hasRole("ADMIN")

                        // POLICY PLANS

                        // View active plans
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/plans/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "AGENT",
                                "CUSTOMER"
                        )

                        // Mutations admin only
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/plans/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/plans/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/plans/**"
                        )
                        .hasRole("ADMIN")

                        // CUSTOMERS

                        // Customer own profile APIs
                        .requestMatchers(
                                "/api/customers/profile/**"
                        )
                        .hasRole("CUSTOMER")

                        // Admin and agent customer lookup
                        .requestMatchers(
                                "/api/customers/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "AGENT"
                        )

                        // POLICIES

                        // Customer buys policy
                        .requestMatchers(
                                "/api/policies/purchase/**"
                        )
                        .hasRole("CUSTOMER")

                        // Agent/Admin issue policy
                        .requestMatchers(
                                "/api/policies/issue/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "AGENT"
                        )

                        // Other policy operations
                        .requestMatchers(
                                "/api/policies/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "AGENT",
                                "CUSTOMER"
                        )

                        // PAYMENTS

                        .requestMatchers(
                                "/api/premium-payments/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "AGENT",
                                "CUSTOMER"
                        )

                        // CLAIMS

                        // Customer raises/views own claims
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/claims/**"
                        )
                        .hasRole("CUSTOMER")

                        // Agent review
                        .requestMatchers(
                                "/api/claims/*/review"
                        )
                        .hasRole("AGENT")

                        // Admin final approval/rejection
                        .requestMatchers(
                                "/api/claims/*/decision"
                        )
                        .hasRole("ADMIN")

                        // remaining claim APIs
                        .requestMatchers(
                                "/api/claims/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "AGENT",
                                "CUSTOMER"
                        )

                        // CLAIM HISTORY

                        .requestMatchers(
                                "/api/claim-history/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "AGENT",
                                "CUSTOMER"
                        )
                        .requestMatchers(
                        		"/api/otp/**"
                        )
                        .permitAll()

                        // anything else requires login
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
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource(){

    CorsConfiguration configuration=new CorsConfiguration();

    configuration.setAllowedOrigins(
            List.of("http://localhost:5173")
    );

    configuration.setAllowedMethods(
            List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS")
    );

    configuration.setAllowedHeaders(
            List.of("*")
    );

    configuration.setAllowCredentials(true);


    UrlBasedCorsConfigurationSource source=
            new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration(
            "/**",
            configuration
    );

    return source;

    }
}