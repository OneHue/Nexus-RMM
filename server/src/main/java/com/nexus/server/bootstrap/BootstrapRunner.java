package com.nexus.server.bootstrap;

import com.nexus.server.bootstrap.service.InitialSetupService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.Console;
import java.util.Arrays;

@Component
@ConditionalOnProperty(
        name = "nexus.bootstrap",
        havingValue = "true"
)
public class BootstrapRunner implements ApplicationRunner {

    private final InitialSetupService setup;

    public BootstrapRunner(InitialSetupService setup) {
        this.setup = setup;
    }

    @Override
    public void run(ApplicationArguments args) {

        Console console = System.console();

        if (console == null) {
            throw new IllegalStateException("Bootstrap requires an interactive terminal");
        }

        console.printf("%nNEXUS INITIAL SETUP%n");
        console.printf("-------------------%n");

        String email = console.readLine("Owner email: ");
        String name = console.readLine("Display name: ");
        String workspace = console.readLine("Workspace name: ");
        String slug = console.readLine("Workspace slug: ");

        char[] password = console.readPassword("Owner password: ");
        char[] confirmation = console.readPassword("Confirm password: ");

        try {
            if (password == null || confirmation == null || !Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("Passwords do not match");
            }

            var result = setup.initialize(
                    email,
                    name,
                    new String(password),
                    workspace,
                    slug
            );

            console.printf("%nNexus initialized successfully.%n");
            console.printf("Workspace ID: %s%n", result.workspaceId());

            console.printf("Owner ID: %s%n", result.userId());

        } finally {

            if (password != null) {
                Arrays.fill(password, '\0');
            }

            if (confirmation != null) {
                Arrays.fill(confirmation, '\0');
            }

        }

    }

}
