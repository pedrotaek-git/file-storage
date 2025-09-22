package com.digitalarkcorp.filestorage.testdouble;

import com.digitalarkcorp.filestorage.domain.ports.StoragePort;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStoragePort implements StoragePort {
    private final Map<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public String initiate(String objectKey) {
        return objectKey;
    }

    @Override
    public void uploadPart(String uploadId, int partNumber, InputStream data, long size) {
        try {
            byte[] bytes = data.readAllBytes();
            store.put(uploadId, bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void complete(String uploadId) {
        // no-op
    }

    @Override
    public InputStream get(String objectKey) {
        byte[] bytes = store.get(objectKey);
        return new ByteArrayInputStream(bytes == null ? new byte[0] : bytes);
    }

    @Override
    public void delete(String objectKey) {
        store.remove(objectKey);
    }
}
