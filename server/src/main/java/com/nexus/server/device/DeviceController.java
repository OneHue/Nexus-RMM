
package com.nexus.server.device;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/devices")
public class DeviceController {

    private final DeviceAccessService deviceAccess;

    public DeviceController(DeviceAccessService deviceAccess) {
        this.deviceAccess = deviceAccess;
    }

    @GetMapping
    public List<DeviceResponse> listDevices(
            @PathVariable UUID workspaceId,
            Authentication authentication
    ) {
        return deviceAccess.listDevices(
                        authentication,
                        workspaceId
                ).stream()
                .map(DeviceController::toResponse)
                .toList();
    }

    @GetMapping("/{deviceId}")
    public DeviceResponse getDevice(
            @PathVariable UUID workspaceId,
            @PathVariable UUID deviceId,
            Authentication authentication
    ) {
        Device device = deviceAccess.getDevice(
                authentication,
                workspaceId,
                deviceId
        );

        return toResponse(device);
    }

    @PatchMapping("/{deviceId}")
    public DeviceResponse renameDevice(
            @PathVariable UUID workspaceId,
            @PathVariable UUID deviceId,
            @RequestBody RenameDeviceRequest request,
            Authentication authentication
    ) {
        Device device = deviceAccess.renameDevice(
                authentication,
                workspaceId,
                deviceId,
                request == null ? null : request.name()
        );

        return toResponse(device);
    }

    private static DeviceResponse toResponse(Device device) {
        return new DeviceResponse(
                device.id(),
                device.workspaceId(),
                device.name(),
                device.hostname(),
                device.operatingSystem(),
                device.createdAt(),
                device.lastSeenAt()
        );
    }

    public record RenameDeviceRequest(
            String name
    ) {
    }

    public record DeviceResponse(
            UUID id,
            UUID workspaceId,
            String name,
            String hostname,
            String operatingSystem,
            Instant createdAt,
            Instant lastSeenAt
    ) {
    }
}
