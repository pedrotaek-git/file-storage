package com.digitalarkcorp.filestorage.infrastructure.fs;

import com.digitalarkcorp.filestorage.domain.ports.StoragePort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class LocalStorageAdapter implements StoragePort {
    private final Path root;

    public LocalStorageAdapter(Path root) {
        this.root = root;
    }

    private Path resolve(String key) {
        return root.resolve(key).normalize();
    }

    @Override
    public void put(String objectKey, InputStream data, long contentLength, String contentType) {
        Path target = resolve(objectKey);
        try {
            Path parent = target.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Resource get(String objectKey) {
        Path p = resolve(objectKey);
        try {
            if (!Files.isRegularFile(p)) {
                throw new RuntimeException("object not found: " + objectKey);
            }
            long len = Files.size(p);
            String ct = Files.probeContentType(p);
            if (ct == null || ct.isBlank()) ct = "application/octet-stream";
            return new Resource(Files.newInputStream(p), len, ct);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String objectKey) {
        Path p = resolve(objectKey);
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
