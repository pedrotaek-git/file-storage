package com.digitalarkcorp.filestorage.infrastructure.s3;

import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import com.digitalarkcorp.filestorage.infrastructure.config.StorageProperties;
import io.minio.*;
import io.minio.errors.MinioException;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class S3StorageAdapter implements StoragePort {

    private final MinioClient client;
    private final StorageProperties props;
    private final Map<String, TempUpload> uploads = new ConcurrentHashMap<>();
    private final AtomicBoolean bucketEnsured = new AtomicBoolean(false);

    public S3StorageAdapter(MinioClient client, StorageProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public String initiate(String objectKey) {
        ensureBucketOnce();
        String uploadId = UUID.randomUUID().toString();
        Path temp;
        try {
            temp = Files.createTempFile("upload-", ".part");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp file", e);
        }
        uploads.put(uploadId, new TempUpload(objectKey, temp));
        return uploadId;
    }

    @Override
    public void uploadPart(String uploadId, int partNumber, InputStream data, long size) {
        TempUpload u = uploads.get(uploadId);
        if (u == null) throw new IllegalStateException("Unknown uploadId: " + uploadId);
        try (OutputStream out = Files.newOutputStream(u.tempFile(), java.nio.file.StandardOpenOption.APPEND)) {
            StreamUtils.copy(data, out);
        } catch (IOException e) {
            throw new RuntimeException("Failed to append part", e);
        }
    }

    @Override
    public void complete(String uploadId) {
        ensureBucketOnce();
        TempUpload u = uploads.remove(uploadId);
        if (u == null) throw new IllegalStateException("Unknown uploadId: " + uploadId);
        try (InputStream in = Files.newInputStream(u.tempFile())) {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(props.bucket())
                            .object(u.objectKey())
                            .stream(in, -1, 10 * 1024 * 1024)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to complete upload", e);
        } finally {
            try { Files.deleteIfExists(u.tempFile()); } catch (IOException ignored) {}
        }
    }

    @Override
    public InputStream get(String objectKey) {
        ensureBucketOnce();
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object: " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        ensureBucketOnce();
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete object: " + objectKey, e);
        }
    }

    private void ensureBucketOnce() {
        if (bucketEnsured.get()) return;
        synchronized (bucketEnsured) {
            if (bucketEnsured.get()) return;
            try {
                boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(props.bucket()).build());
                if (!exists) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(props.bucket()).build());
                }
                bucketEnsured.set(true);
            } catch (MinioException | IOException | RuntimeException e) {
                throw new RuntimeException("Failed to ensure bucket: " + props.bucket(), e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private record TempUpload(String objectKey, Path tempFile) {}
}
