package com.monocept.project.security;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService customUserDetailsService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
        this.resolver = resolver;
    }

    // Public auth endpoints (login, register, forgot/reset password) never need
    // a token. Skipping them means a stale token sent along with the request
    // (e.g. from Postman after a password reset) can't block logging in again.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwtToken = authHeader.substring(7);
        final String userEmail;

        try {
            userEmail = jwtService.extractUsername(jwtToken);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);

                // Deactivated after the token was issued -> locked out immediately.
                if (!userDetails.isEnabled()) {
                    log.warn("LOG-014 Security failure. Rejected request from a deactivated account");
                    SecurityContextHolder.clearContext();
                    resolver.resolveException(request, response, null,
                            new com.monocept.project.exception.AuthenticationException(
                                    "This account has been deactivated. Please contact support."));
                    return;
                }

                // Password changed after this token was issued (e.g. a reset from
                // another device) -> this session must sign in again.
                if (userDetails instanceof CustomUserDetails customUser
                        && issuedBeforePasswordChange(jwtToken, customUser)) {
                    log.warn("LOG-014 Security failure. Rejected token issued before the latest password change");
                    SecurityContextHolder.clearContext();
                    resolver.resolveException(request, response, null,
                            new InvalidJwtTokenException("Your password was changed. Please sign in again."));
                    return;
                }

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

            filterChain.doFilter(request, response);

        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.warn("LOG-014 Security failure. Expired JWT token");
            SecurityContextHolder.clearContext();
            resolver.resolveException(request, response, null,
                    new ExpiredJwtTokenException("Token has expired. Please sign in again."));
        } catch (UsernameNotFoundException ex) {
            // The token's subject (email) no longer matches an account — typically
            // the email was changed from My Profile or by an admin. Previously this
            // escaped the filter and surfaced as a 500 instead of a clean 401.
            log.warn("LOG-014 Security failure. Token subject no longer matches an account");
            SecurityContextHolder.clearContext();
            resolver.resolveException(request, response, null,
                    new InvalidJwtTokenException("Your session is no longer valid. Please sign in again."));
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            log.warn("LOG-014 Security failure. Malformed or tampered JWT token");
            SecurityContextHolder.clearContext();
            resolver.resolveException(request, response, null,
                    new InvalidJwtTokenException("Invalid token verification signature."));
        }
    }

    private boolean issuedBeforePasswordChange(String jwtToken, CustomUserDetails customUser) {
        if (customUser.getUser().getPasswordChangedAt() == null) {
            return false;
        }
        Date issuedAt = jwtService.extractIssuedAt(jwtToken);
        if (issuedAt == null) {
            return true;
        }
        // JWT "iat" has whole-second precision, so compare at second precision.
        long changedAtSeconds = customUser.getUser().getPasswordChangedAt()
                .atZone(ZoneId.systemDefault()).toEpochSecond();
        long issuedAtSeconds = issuedAt.getTime() / 1000;
        return issuedAtSeconds < changedAtSeconds;
    }
}