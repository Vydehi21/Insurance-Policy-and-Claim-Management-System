package com.monocept.project.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.monocept.project.exception.ExpiredJwtTokenException;
import com.monocept.project.exception.InvalidJwtTokenException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final HandlerExceptionResolver resolver;

    // 🛠️ Constructor routing with @Qualifier keeps Spring Boot from experiencing generic bean injection ambiguity
    public JwtAuthenticationFilter(
            JwtService jwtService, 
            CustomUserDetailsService customUserDetailsService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwtToken;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwtToken = authHeader.substring(7);

        try {
            userEmail = jwtService.extractUsername(jwtToken);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwtToken, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
            
            // Proceed to the next filter if token authentication succeeded smoothly
            filterChain.doFilter(request, response);

        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.warn("JWT token expiration detected during request intercept filter parsing path");
            SecurityContextHolder.clearContext();
            // 🛠️ Routes the exception cleanly back onto GlobalExceptionHandler entries
            resolver.resolveException(request, response, null, new ExpiredJwtTokenException("Token has expired. Please sign in again."));
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            log.warn("Malformed or broken security token signature intercepted");
            SecurityContextHolder.clearContext();
            // 🛠️ Routes the exception cleanly back onto GlobalExceptionHandler entries
            resolver.resolveException(request, response, null, new InvalidJwtTokenException("Invalid token verification signature."));
        }
    }
}
