
package com.nexus.server.persistence;

import com.nexus.server.workspace.Workspace;
import com.nexus.server.workspace.repository.WorkspaceRepository;
import com.nexus.server.device.Device;
import com.nexus.server.device.repository.DeviceRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RepositoryIntegrationTest {

    @Autowired
    private WorkspaceRepository workspaces;

    @Autowired
    private DeviceRepository devices;

    @Test
    void canCreateReadAndUpdateDevices() {

        // Create a workspace
        Workspace workspace = workspaces.create(
                "Test Infrastructure",
                "test-" + UUID.randomUUID()
        );

        assertNotNull(workspace.id());

        // Retrieve the workspace from PostgreSQL
        Workspace savedWorkspace = workspaces
                .findById(workspace.id())
                .orElseThrow();

        assertEquals(workspace, savedWorkspace);

        // Create a device belonging to the workspace
        Device device = devices.create(
                workspace.id(),
                "Main PC",
                "DESKTOP-01",
                "Windows"
        );

        assertNotNull(device.id());
        assertEquals(workspace.id(), device.workspaceId());
        assertNull(device.lastSeenAt());

        // Retrieve the device
        Device savedDevice = devices
                .findById(workspace.id(), device.id())
                .orElseThrow();

        assertEquals(device, savedDevice);

        // Rename the device
        boolean updated = devices.rename(
                workspace.id(),
                device.id(),
                "Gaming PC"
        );

        assertTrue(updated);

        Device renamedDevice = devices
                .findById(workspace.id(), device.id())
                .orElseThrow();

        assertEquals("Gaming PC", renamedDevice.name());

        // Verify workspace device listing
        assertTrue(
                devices.findAllByWorkspaceId(workspace.id())
                        .contains(renamedDevice)
        );

        // Verify workspace isolation
        Workspace anotherWorkspace = workspaces.create(
                "Another Infrastructure",
                "another-" + UUID.randomUUID()
        );

        assertTrue(
                devices.findById(
                        anotherWorkspace.id(),
                        device.id()
                ).isEmpty()
        );

        assertFalse(
                devices.rename(
                        anotherWorkspace.id(),
                        device.id(),
                        "Unauthorized Rename"
                )
        );
    }
}
