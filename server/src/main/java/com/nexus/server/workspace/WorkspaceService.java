package com.nexus.server.workspace;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class WorkspaceService {

    private final WorkspaceRepository repository;

    public WorkspaceService(WorkspaceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Workspace createWorkspace(String name, String slug) {

        String validName = requireText(name, "Workspace name", 100);

        String validSlug = requireText(slug, "Workspace slug", 100)
                .toLowerCase(Locale.ROOT);

        if (!validSlug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new IllegalArgumentException(
                    "Workspace slug must only contain lowercase letters, numbers, or hyphens"
            );
        }

        if (repository.findBySlug(validSlug).isPresent()) {
            throw new IllegalArgumentException(
                    "A workspace with this slug already exists"
            );
        }

        return repository.create(validName, validSlug);
    }

    @Transactional(readOnly = true)
    public Workspace getWorkspace(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Workspace ID is required"
            );
        }

        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                "Workspace not found"
        ));
    }

    private static String requireText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " cannot be empty"
            );
        }

        String cleaned = value.trim();

        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " cannot exceed " + maxLength + " characters"
            );
        }

        return cleaned;
    }
}
