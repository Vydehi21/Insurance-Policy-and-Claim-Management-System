package com.monocept.project.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
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
                SessionCreationPolicy.STATELESS
            )
        )


        .authorizeHttpRequests(auth -> auth



            // PUBLIC APIs
            .requestMatchers(
                    "/api/auth/**",
                    "/api/otp/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/error"
            )
            .permitAll()



            // =========================
            // PRODUCTS & PLANS
            // =========================

            // customer can browse
            // admin can manage
            .requestMatchers(
                    "/api/products/**",
                    "/api/plans/**"
            )
            .hasAnyRole(
                    "ADMIN",
                    "INTERNAL_STAFF",
                    "CUSTOMER"
            )



            // =========================
            // ADMIN MANAGEMENT
            // =========================

            .requestMatchers(
                    "/api/users/**"
            )
            .hasRole("ADMIN")
            
         // CUSTOMER OWN PROFILE
            .requestMatchers(
                    "/api/customers/profile"
            )
            .hasRole("CUSTOMER")
            .requestMatchers(
                    "/api/customers/me"
            )
            .hasRole("CUSTOMER")


            .requestMatchers(
                    "/api/customers/**"
            )
            .hasAnyRole(
                    "ADMIN",
                    "INTERNAL_STAFF",
                    "CUSTOMER"
            )




            // =========================
            // CUSTOMER POLICIES
            // =========================

            // customer own policies
            .requestMatchers(
                    "/api/policies/my"
            )
            .hasRole("CUSTOMER")

            .requestMatchers(
                    "/api/policies/issue"
            )
            .hasAnyRole("ADMIN",
                    "INTERNAL_STAFF")
            
            .requestMatchers(
                    "/api/policies/internal-staff"
            )
            .hasRole("INTERNAL_STAFF")
            
         // customer purchase policy
            .requestMatchers(
                    "/api/policies/purchase"
            )
            .hasRole("CUSTOMER")

            // remaining policy endpoints share role/ownership enforcement
            // between @PreAuthorize on the controller and the service-layer
            // ownership checks in PolicyServiceImpl
            .requestMatchers(
                    "/api/policies/**"
            )
            .hasAnyRole("ADMIN", "INTERNAL_STAFF", "CUSTOMER")




            


            // =========================
            // CLAIMS
            // =========================


            // customer sees own claims
            .requestMatchers(
                    "/api/claims/my"
            )
            .hasRole("CUSTOMER")



            // admin sees all claims


            // internal staff review claim
            .requestMatchers(
                    "/api/claims/*/review"
            )
            .hasRole("INTERNAL_STAFF")



            // admin final decision
            .requestMatchers(
                    "/api/claims/*/decision"
            )
            .hasRole("ADMIN")

            .requestMatchers(
                    "/api/files/upload"
            )
            .hasRole("CUSTOMER")

            // customer create claim

            .requestMatchers(HttpMethod.POST, "/api/claims")
            .hasRole("CUSTOMER")

            // remaining claim endpoints share role/ownership enforcement
            // between @PreAuthorize on the controller and the service-layer
            // ownership checks added in ClaimServiceImpl
            .requestMatchers(
                    "/api/claims/**"
            )
            .hasAnyRole("ADMIN", "INTERNAL_STAFF", "CUSTOMER")




            // =========================
            // PREMIUM PAYMENTS
            // =========================


            .requestMatchers(
                    "/api/premium-payments/internal-staff"
            )
            .hasAnyRole("ADMIN", "INTERNAL_STAFF")

            // customer pays and views own payments
            .requestMatchers(
                    "/api/premium-payments/my"
            )
            .hasRole("CUSTOMER")

            // remaining payment endpoints are shared by all three roles;
            // per-role and per-owner enforcement happens via @PreAuthorize
            // on the controller and the ownership checks in
            // PremiumPaymentServiceImpl
            .requestMatchers(
                    "/api/premium-payments/**"
            )
            .hasAnyRole("ADMIN", "INTERNAL_STAFF", "CUSTOMER")

            // =========================
            // CLAIM HISTORY
            // =========================

            .requestMatchers(
                    "/api/claim-history/**"
            )
            .hasAnyRole(
                    "ADMIN",
                    "INTERNAL_STAFF",
                    "CUSTOMER"
            )



            // everything else
            .anyRequest()
            .authenticated()

        )



        .httpBasic(httpBasic ->
                httpBasic.disable()
        )


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