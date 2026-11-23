package com.nexus.server.device.service;

import com.nexus.server.device.Device;
import com.nexus.server.device.repository.DeviceRepository;
import com.nexus.server.workspace.service.WorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class DeviceService {

    private final DeviceRepository repository;
    private final WorkspaceService workspaceService;

    public DeviceService(
            DeviceRepository repository,
            WorkspaceService workspaceService
    ) {
        this.repository = repository;
        this.workspaceService = workspaceService;
    }

    @Transactional
    public Device addDevice(
            UUID workspaceId,
            String name,
            String hostname,
            String operatingSystem
    ) {
        // getWorkspace checks to see if the workspace exists
        // along with returning any existing workspace with that id
        // if this fails it allows us to know right away instead of running
        // into issues down the run.
        workspaceService.getWorkspace(workspaceId);

        String validName = requireName(name);
        String validHostname = optionalText(hostname, 255);
        String validOS = optionalText(operatingSystem, 120);

        return repository.create(
                workspaceId,
                validName,
                validHostname,
                validOS
        );
    }

    @Transactional(readOnly = true)
    public Device getDevice(UUID workspaceId, UUID deviceId) {

        requireIds(workspaceId, deviceId);

        return repository.findById(workspaceId, deviceId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Device not found"
                ));
    }

    @Transactional(readOnly = true)
    public List<Device> listDevices(UUID workspaceId) {
        workspaceService.getWorkspace(workspaceId);

        return repository.findAllByWorkspaceId(workspaceId);
    }

    @Transactional
    public Device renameDevice(
            UUID workspaceId,
            UUID deviceId,
            String newName
    ) {
        requireIds(workspaceId, deviceId);

        String validName = requireName(newName);

        boolean updated = repository.rename(
                workspaceId,
                deviceId,
                validName
        );

        if (!updated) {
            throw new NoSuchElementException("Device not found");
        }

        return repository.findById(workspaceId, deviceId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Device not found"
                ));
    }

    private static String requireName(String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Device name cannot be empty"
            );
        }

        String cleaned = name.trim();

        if (cleaned.length() > 120) {
            throw new IllegalArgumentException(
                    "Device name cannot exceed 120 characters"
            );
        }

        return cleaned;

    }

    private String optionalText(String value, int maxLength) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String cleaned = value.trim();

        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException(
                    "Field cannot exceed " + maxLength + " characters"
            );
        }

        return cleaned;

    }

    private void requireIds(UUID workspaceId, UUID deviceId) {
        if (workspaceId == null || deviceId == null) {
            throw new IllegalArgumentException(
                    "Workspace ID and Device are required"
            );
        }
    }


}
