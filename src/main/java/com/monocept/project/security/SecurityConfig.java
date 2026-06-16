package com.monocept.project.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
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
                SessionCreationPolicy.STATELESS
            )

        )


        .authorizeHttpRequests(auth -> auth


            .requestMatchers(
                    "/api/auth/**",
                    "/api/otp/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/error"
            )

            .permitAll()



            .requestMatchers(
                    "/api/products/**",
                    "/api/plans/**",
                    "/api/users/**",
                    "/api/customers/**"
            )

            .hasRole("ADMIN")



            .requestMatchers(
                    "/api/policies/purchase",
                    "/api/premium-payments/**",
                    "/api/claims/**"
            )

            .hasRole("CUSTOMER")



            .requestMatchers(
                    "/api/claims/*/review"
            )

            .hasRole("AGENT")



            .requestMatchers(
                    "/api/claims/*/decision"
            )

            .hasRole("ADMIN")



            .requestMatchers(
                    "/api/claim-history/**"
            )

            .hasAnyRole(
                    "ADMIN",
                    "AGENT"
            )



            .anyRequest()
            .authenticated()

        )



        // REMOVE BASIC LOGIN POPUP
        .httpBasic(httpBasic ->
                httpBasic.disable()
        )



        // REMOVE DEFAULT LOGIN PAGE
        .formLogin(form ->
                form.disable()
        )


        .addFilterBefore(

                jwtAuthenticationFilter,

                UsernamePasswordAuthenticationFilter.class

        );



        return http.build();

    }





    @Bean
    PasswordEncoder passwordEncoder(){

        return new BCryptPasswordEncoder();

    }





    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {


        return configuration.getAuthenticationManager();

    }

}