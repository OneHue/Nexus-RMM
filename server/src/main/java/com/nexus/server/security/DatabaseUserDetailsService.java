package com.nexus.server.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserAuthenticationRepository repository;

    public DatabaseUserDetailsService(UserAuthenticationRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException(
                    "Invalid Credentials"
            );
        }

        String email = username.trim().toLowerCase(Locale.ROOT);

        return repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Invalid Credentials"
                ));

    }

}
