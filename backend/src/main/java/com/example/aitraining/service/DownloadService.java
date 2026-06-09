package com.example.aitraining.service;

import com.example.aitraining.config.AppProperties;
import com.example.aitraining.repo.SupportRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.UUID;

@Service
public class DownloadService {
    private final SupportRepository support;
    private final Path root;

    public DownloadService(SupportRepository support, AppProperties props) {
        this.support = support;
        this.root = Path.of(props.storageRoot()).toAbsolutePath().normalize();
    }

    public Resource artifact(UUID artifactId) {
        return safeResource(support.artifactPath(artifactId));
    }

    public Resource safeResource(String path) {
        Path resolved = root.resolve(path).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Unsafe storage path");
        }
        return new FileSystemResource(resolved);
    }
}
