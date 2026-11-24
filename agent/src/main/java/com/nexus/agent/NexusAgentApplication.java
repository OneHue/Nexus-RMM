package com.nexus.agent;

import tools.jackson.databind.json.JsonMapper;

import java.io.Console;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

public class NexusAgentApplication {

    private static final String SERVER = "http://127.0.0.1:8080";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    public static void main(String[] args) throws Exception {
        Console console = System.console();

        if (console == null) {
            throw new IllegalStateException("Run the agent from an interactive terminal");
        }

        System.out.println("NEXUS AGENT");
        System.out.println("-----------");

        char[] input = console.readPassword("Enrollment code: ");

        if (input == null || input.length == 0) {
            throw new IllegalArgumentException("Enrollment code is required");
        }

        String code;
        String hostname;

        try {
            code = new String(input).trim();
        } finally {
            Arrays.fill(input, '\0');
        }

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostname = "unknown-host";
        }

        String os = System.getProperty("os.name");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        String enrollmentBody = JSON.writeValueAsString(
                new EnrollmentRequest(
                        code,
                        hostname,
                        os
                )
        );

        HttpRequest enrollmentRequest = HttpRequest.newBuilder(URI.create(SERVER + "/api/v1/agent/enroll"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(enrollmentBody))
                .build();

        HttpResponse<String> enrollmentResponse = client.send(
                enrollmentRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        if (enrollmentResponse.statusCode() != 201) {
            throw new IllegalStateException("Enrollment failed: HTTP "
                    + enrollmentResponse.statusCode());
        }

        EnrollmentResponse enrollment =
                JSON.readValue(
                        enrollmentResponse.body(),
                        EnrollmentResponse.class
                );

        System.out.println("Device enrolled: " + enrollment.deviceId());

        String credential = enrollment.credential();

        for (int i = 1; i <= 3; i++) {

            HttpRequest heartbeat =
                    HttpRequest.newBuilder(
                                    URI.create(
                                            SERVER + "/api/v1/agent/heartbeat"
                                    )
                            )
                            .timeout(Duration.ofSeconds(10))
                            .header(
                                    "Authorization",
                                    "Bearer " + credential
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.noBody()
                            )
                            .build();

            HttpResponse<Void> response =
                    client.send(
                            heartbeat,
                            HttpResponse.BodyHandlers.discarding()
                    );

            if (response.statusCode() != 204) {
                throw new IllegalStateException(
                        "Heartbeat failed: HTTP "
                                + response.statusCode()
                );
            }

            System.out.println(
                    "Heartbeat " + i + "/3 successful"
            );

            if (i < 3) {
                Thread.sleep(15_000);
            }
        }

        System.out.println(
                "Agent connection test completed."
        );

        // Credential is deliberately not persisted yet.
    }

    public record EnrollmentRequest(
            String code,
            String hostname,
            String operatingSystem
    ) { }

    public record EnrollmentResponse(
            UUID deviceId,
            UUID workspaceId,
            String credential
    ) { }
}
