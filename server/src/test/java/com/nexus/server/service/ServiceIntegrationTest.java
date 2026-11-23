
package com.nexus.server.service;

import com.nexus.server.workspace.Workspace;
import com.nexus.server.workspace.service.WorkspaceService;
import com.nexus.server.device.Device;
import com.nexus.server.device.service.DeviceService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ServiceIntegrationTest {

    @Autowired
    private WorkspaceService workspaces;

    @Autowired
    private DeviceService devices;

    @Test
    void canManageWorkspacesAndDevices() {

        Workspace workspace = workspaces.createWorkspace(
                "Home Infrastructure",
                "home-" + UUID.randomUUID()
        );

        assertNotNull(workspace.id());

        Device device = devices.addDevice(
                workspace.id(),
                "  Main PC  ",
                "DESKTOP-01",
                "Windows 11"
        );

        // The service should trim device names.
        assertEquals("Main PC", device.name());

        // Verify device retrieval.
        Device saved = devices.getDevice(
                workspace.id(),
                device.id()
        );

        assertEquals(device, saved);

        // Verify renaming.
        Device renamed = devices.renameDevice(
                workspace.id(),
                device.id(),
                "Gaming Desktop"
        );

        assertEquals("Gaming Desktop", renamed.name());

        // Verify listing.
        assertTrue(
                devices.listDevices(workspace.id())
                        .contains(renamed)
        );

        // Create another workspace.
        Workspace other = workspaces.createWorkspace(
                "Other Infrastructure",
                "other-" + UUID.randomUUID()
        );

        // The other workspace cannot retrieve this device.
        assertThrows(
                NoSuchElementException.class,
                () -> devices.getDevice(
                        other.id(),
                        device.id()
                )
        );

        // Nor can it rename the device.
        assertThrows(
                NoSuchElementException.class,
                () -> devices.renameDevice(
                        other.id(),
                        device.id(),
                        "Changed"
                )
        );
    }

    @Test
    void rejectsInvalidInformation() {

        assertThrows(
                IllegalArgumentException.class,
                () -> workspaces.createWorkspace("", "invalid")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> workspaces.createWorkspace(
                        "Test Workspace",
                        "invalid slug"
                )
        );

        Workspace workspace = workspaces.createWorkspace(
                "Valid Workspace",
                "valid-" + UUID.randomUUID()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> devices.addDevice(
                        workspace.id(),
                        "  ",
                        null,
                        null
                )
        );

        assertThrows(
                NoSuchElementException.class,
                () -> devices.addDevice(
                        UUID.randomUUID(),
                        "Unknown PC",
                        null,
                        null
                )
        );
    }
}

