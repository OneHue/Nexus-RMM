package com.nexus.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// This annotation alerts SpringBoot to auto configure the SpringBoot application
@SpringBootApplication
public class NexusServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusServerApplication.class, args);
    }
}
