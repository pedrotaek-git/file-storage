package com.digitalarkcorp.filestorage.testdouble;

import com.digitalarkcorp.filestorage.domain.ports.StoragePort;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FakeStoragePort implements StoragePort {

    private static class UploadBuf {
        final String objectKey;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        UploadBuf(String objectKey) { this.objectKey = objectKey; }
    }

    private final Map<String, UploadBuf> uploads = new ConcurrentHashMap<>();
    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public String initiate(String objectKey) {
        String uploadId = UUID.randomUUID().toString();
        uploads.put(uploadId, new UploadBuf(objectKey));
        return uploadId;
    }

    @Override
    public void uploadPart(String uploadId, int partNumber, InputStream data, long size) {
        UploadBuf ub = uploads.get(uploadId);
        if (ub == null) throw new IllegalStateException("unknown uploadId");
        try (data) {
            data.transferTo(ub.buffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void complete(String uploadId) {
        UploadBuf ub = uploads.remove(uploadId);
        if (ub == null) throw new IllegalStateException("unknown uploadId");
        objects.put(ub.objectKey, ub.buffer.toByteArray());
    }

    @Override
    public InputStream get(String objectKey) {
        byte[] data = objects.get(objectKey);
        if (data == null) throw new IllegalArgumentException("not found: " + objectKey);
        return new ByteArrayInputStream(data);
    }

    @Override
    public void delete(String objectKey) {
        objects.remove(objectKey);
    }
}
