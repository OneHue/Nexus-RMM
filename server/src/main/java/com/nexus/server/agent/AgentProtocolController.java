package com.nexus.server.agent;


import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentProtocolController {

    private final AgentEnrollmentService enrollment;
    private final JdbcTemplate jdbc;

    public AgentProtocolController(
            AgentEnrollmentService enrollment,
            JdbcTemplate jdbc
    ) {
        this.enrollment = enrollment;
        this.jdbc = jdbc;
    }

    public record EnrollmentRequest(
            String code,
            String hostname,
            String operatingSystem
    ) {}

    @PostMapping("/enroll")
    public ResponseEntity<AgentEnrollmentService.EnrollmentResult> enroll(@RequestBody EnrollmentRequest request) {
        if (request == null) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST);}

        try {
            var result = enrollment.enroll(
                    request.code(),
                    request.hostname(),
                    request.operatingSystem()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .cacheControl(CacheControl.noStore())
                    .body(result);
        } catch (AgentEnrollmentService.InvalidEnrollmentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired enrollment code");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid enrollment request");
        }
    }

    @PostMapping("/heartbeat")
    @Transactional
    public ResponseEntity<Void> heartbeat(Authentication authentication) {
        AgentPrincipal agent = (AgentPrincipal) authentication.getPrincipal();

        int updated = jdbc.update("""
                UPDATE devices
                SET last_seen_at = NOW()
                WHERE id = ?
                    AND workspace_id = ?
                """,
                agent.deviceId(),
                agent.workspaceId()
        );

        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return ResponseEntity.noContent().build();
    }



}
