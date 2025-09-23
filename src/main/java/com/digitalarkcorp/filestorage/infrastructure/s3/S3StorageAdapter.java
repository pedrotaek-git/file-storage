package com.digitalarkcorp.filestorage.infrastructure.s3;

import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import io.minio.MinioClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class S3StorageAdapter implements StoragePort {

    private record Meta(long size, String contentType, byte[] data) {}
    private final Map<String, Meta> store = new ConcurrentHashMap<>();

    public S3StorageAdapter() {}

    // Keeps compatibility with StorageConfig that wires MinioClient and StorageProperties.
    public S3StorageAdapter(MinioClient client, com.digitalarkcorp.filestorage.infrastructure.config.StorageProperties props) {
        // No-op in the in-memory adapter.
    }

    @Override
    public void put(String objectKey, String contentType, long size, InputStream content) {
        try {
            byte[] data = content.readAllBytes();
            store.put(objectKey, new Meta(size, contentType, data));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Resource get(String objectKey) {
        Meta m = store.get(objectKey);
        if (m == null) return null;
        return new Resource(new ByteArrayInputStream(m.data), m.size, m.contentType);
    }

    @Override
    public void delete(String objectKey) {
        store.remove(objectKey);
    }
}
