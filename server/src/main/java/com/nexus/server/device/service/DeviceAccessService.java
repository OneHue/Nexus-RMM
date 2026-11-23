package com.nexus.server.device.service;


import com.nexus.server.device.Device;
import com.nexus.server.security.service.CurrentUserService;
import com.nexus.server.user.User;
import com.nexus.server.workspace.service.WorkspaceMembershipService;
import com.nexus.server.workspace.WorkspaceRole;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceAccessService {
    private final DeviceService devices;
    private final CurrentUserService users;
    private final WorkspaceMembershipService memberships;

    public DeviceAccessService(DeviceService devices, CurrentUserService users, WorkspaceMembershipService memberships) {
        this.devices = devices;
        this.users = users;
        this.memberships = memberships;
    }

    @Transactional(readOnly = true)
    public List<Device> listDevices(
            Authentication authentication,
            UUID workspaceId
    ) {
        User user = users.requireCurrentUser(authentication);

        memberships.requireMembership(user.id(), workspaceId);

        return devices.listDevices(workspaceId);

    }

    @Transactional(readOnly = true)
    public Device getDevice(
            Authentication authentication,
            UUID workspaceId,
            UUID deviceId
    ) {
        User user = users.requireCurrentUser(authentication);

        memberships.requireMembership(user.id(), workspaceId);

        return devices.getDevice(workspaceId, deviceId);
    }

    @Transactional
    public Device renameDevice(
            Authentication authentication,
            UUID workspaceId,
            UUID deviceId,
            String newName
    ) {
        User user = users.requireCurrentUser(authentication);

        memberships.requireMembership(user.id(), workspaceId);

        memberships.requireRole(
                user.id(),
                workspaceId,
                EnumSet.of(
                        WorkspaceRole.OWNER,
                        WorkspaceRole.ADMIN
                )
        );

        return devices.renameDevice(workspaceId, deviceId, newName);
    }

}