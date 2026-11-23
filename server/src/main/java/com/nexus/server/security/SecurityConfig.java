package com.nexus.server.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
public class SecurityConfig {

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            DatabaseUserDetailsService users,
            PasswordEncoder passwordEncoder
    ) {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwordEncoder);

        return provider;

    }

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
                                        "/api/v1/health",
                                        "/api/v1/auth/csrf"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/auth/login"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/auth/me"
                                ).authenticated()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/auth/logout"
                                ).authenticated()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/workspaces",
                                        "/api/v1/workspaces/{workspaceId}",
                                        "/api/v1/workspaces/{workspaceId}/members"
                                ).authenticated()

                                .anyRequest().denyAll()
                        // anyRequest().denyAll() blocks everything else for now.
                        // Default Deny Security policy is the goal for the scope of Nexus
                )

                // No login mechanism has been deployed yet.
                .formLogin(form -> form
                        .loginPage("/api/v1/auth/login")
                        .loginProcessingUrl("/api/v1/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")

                        .successHandler(((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        }))

                        .failureHandler(((request, response, exception) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "Invalid Credentials"
                            );
                        }))

                ).logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")

                        .logoutSuccessHandler(((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT
                            );
                        }))
                ).exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                (request, response, authException) -> {
                                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                                }
                        ).accessDeniedHandler(
                                (request, response, accessDeniedException) -> {
                                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                                }
                        )
                );

        // Spring Security's default CSRF protection remains enabled.
        return http.build();
    }
}
