package com.digitalarkcorp.filestorage.infrastructure.fs;

import com.digitalarkcorp.filestorage.domain.ports.StoragePort;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class LocalStorageAdapter implements StoragePort {

    private final Path root;

    public LocalStorageAdapter(Path root) {
        this.root = root;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path keyToPath(String objectKey) {
        return root.resolve(objectKey);
    }

    @Override
    public void put(String objectKey, InputStream data, long contentLength, String contentType) {
        Path p = keyToPath(objectKey);
        try {
            Files.createDirectories(p.getParent());
            try (OutputStream out = Files.newOutputStream(
                    p, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                data.transferTo(out);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Resource get(String objectKey) {
        Path p = keyToPath(objectKey);
        try {
            byte[] bytes = Files.readAllBytes(p);
            return new Resource(new ByteArrayInputStream(bytes), bytes.length, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    @Override
    public void delete(String objectKey) {
        Path p = keyToPath(objectKey);
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
