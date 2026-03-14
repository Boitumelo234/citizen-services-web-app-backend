package com.webapp.citizen_services_web_app_backend.security;

import com.webapp.citizen_services_web_app_backend.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (jwtService.isTokenValid(token)) {
                String email = jwtService.extractEmail(token);
                String role = jwtService.extractRole(token);

                // Debug prints – check your Spring Boot console
                System.out.println("JWT Filter - Token valid for email: " + email);
                System.out.println("JWT Filter - Role extracted: " + role);

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                System.out.println("JWT Filter - Authorities granted: " + authorities);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        authorities
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);

                System.out.println("JWT Filter - SecurityContext authorities: " +
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities());
            } else {
                System.out.println("JWT Filter - Token invalid");
            }
        } catch (Exception e) {
            System.out.println("JWT Filter - Error processing token: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}