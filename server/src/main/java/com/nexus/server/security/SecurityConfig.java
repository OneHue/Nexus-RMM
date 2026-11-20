package com.nexus.server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    // This defines the security rules applied to incoming HTTP requests.
    // The Spring Security processes these rules before the request reaches the application's controller.
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth

                                // Public application health endpoint
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/health"
                                ).permitAll().anyRequest().denyAll()

                        // anyRequest().denyAll() blocks everything else for now.
                        // Default Deny Security policy is the goal for the scope of Nexus
                )

                // No login mechanism has been deployed yet.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        // Spring Security's default CSRF protection remains enabled.
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Temporary fail-closed authentication provider
        // Replace when implementing database-backed login
        return username -> {
            throw new UsernameNotFoundException(
                    "Authentication is not configured"
            );
        };
    }
}
