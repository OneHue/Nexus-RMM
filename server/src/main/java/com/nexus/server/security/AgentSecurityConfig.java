package com.nexus.server.security;


import com.nexus.server.agent.AgentAuthenticationFilter;
import com.nexus.server.agent.service.AgentCredentialService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AgentSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain agentSecurityFilterChain(
            HttpSecurity http,
            AgentCredentialService credentials
    ) throws Exception{
        http.securityMatcher("/api/v1/agent/**")
                .sessionManagement
                        (session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                ).csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .addFilterBefore(new AgentAuthenticationFilter(credentials),
                        UsernamePasswordAuthenticationFilter.class
                ).authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/agent/enroll"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/agent/heartbeat"
                        ).hasAuthority("ROLE_NEXUS_AGENT")
                        .anyRequest().denyAll()
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(
                                ((request, response, authException) ->
                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED
                                        ))
                        )
                );

        return http.build();

    }

}
