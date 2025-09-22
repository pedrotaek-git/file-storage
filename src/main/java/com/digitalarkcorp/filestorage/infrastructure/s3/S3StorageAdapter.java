package com.digitalarkcorp.filestorage.infrastructure.s3;

import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import com.digitalarkcorp.filestorage.infrastructure.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class S3StorageAdapter implements StoragePort {

    private static final String MPU_BASE = "/tmp/filestorage-mpu";
    private static final String META_OBJECT_KEY = "object.key";
    private static final Pattern PART_PATTERN = Pattern.compile("^part-(\\d+)$");
    private static final long DEFAULT_MULTIPART_CHUNK = 10L * 1024 * 1024; // 10MB

    private final MinioClient client;
    private final StorageProperties props;

    public S3StorageAdapter(MinioClient client, StorageProperties props) {
        this.client = client;
        this.props = props;
        ensureBucket();
    }

    @Override
    public String initiate(String objectKey) {
        String uploadId = UUID.randomUUID().toString();
        Path dir = sessionDir(uploadId);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(META_OBJECT_KEY), objectKey, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("initiate failed for " + objectKey, e);
        }
        return uploadId;
    }

    @Override
    public void uploadPart(String uploadId, int partNumber, InputStream data, long size) {
        Path dir = sessionDir(uploadId);
        if (!Files.isDirectory(dir)) throw new RuntimeException("upload session not found: " + uploadId);
        Path part = dir.resolve("part-" + partNumber);
        try (OutputStream out = Files.newOutputStream(part)) {
            copyExactly(data, out, size);
        } catch (IOException e) {
            throw new RuntimeException("uploadPart failed: " + uploadId + " #" + partNumber, e);
        }
    }

    @Override
    public void complete(String uploadId) {
        Path dir = sessionDir(uploadId);
        if (!Files.isDirectory(dir)) throw new RuntimeException("upload session not found: " + uploadId);

        final String objectKey;
        try {
            objectKey = Files.readString(dir.resolve(META_OBJECT_KEY), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new RuntimeException("missing object key for " + uploadId, e);
        }

        File[] parts = dir.toFile().listFiles(f -> f.isFile() && f.getName().startsWith("part-"));
        if (parts == null) parts = new File[0];
        if (parts.length == 0) {
            try (InputStream empty = InputStream.nullInputStream()) {
                client.putObject(
                        PutObjectArgs.builder()
                                .bucket(props.bucket())
                                .object(objectKey)
                                .stream(empty, 0, -1)
                                .contentType("application/octet-stream")
                                .build()
                );
            } catch (Exception e) {
                throw new RuntimeException("complete(empty) failed for " + objectKey, e);
            } finally {
                silentDelete(dir.toFile());
            }
            return;
        }

        Arrays.sort(parts, Comparator.comparingInt(S3StorageAdapter::partNumberOf));

        List<InputStream> inputs = new ArrayList<>(parts.length);
        try {
            for (File p : parts) inputs.add(new BufferedInputStream(new FileInputStream(p)));
            try (InputStream concat = new SequenceInputStream(Collections.enumeration(inputs))) {
                client.putObject(
                        PutObjectArgs.builder()
                                .bucket(props.bucket())
                                .object(objectKey)
                                .stream(concat, -1, DEFAULT_MULTIPART_CHUNK) // unknown size, set partSize
                                .contentType("application/octet-stream")
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("complete failed for " + objectKey, e);
        } finally {
            for (InputStream in : inputs) try { in.close(); } catch (IOException ignored) {}
            silentDelete(dir.toFile());
        }
    }

    @Override
    public InputStream get(String objectKey) {
        try {
            return client.getObject(
                    GetObjectArgs.builder()
                            .bucket(props.bucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("get failed for " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(props.bucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("delete failed for " + objectKey, e);
        }
    }

    /* ---------------- helpers ---------------- */

    private void ensureBucket() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(props.bucket()).build());
            if (!exists) client.makeBucket(MakeBucketArgs.builder().bucket(props.bucket()).build());
        } catch (Exception e) {
            throw new RuntimeException("bucket ensure failed: " + props.bucket(), e);
        }
    }

    private static Path sessionDir(String uploadId) {
        String safe = uploadId.replaceAll("[^a-zA-Z0-9._-]", "_");
        return Path.of(MPU_BASE, safe);
    }

    private static int partNumberOf(File f) {
        Matcher m = PART_PATTERN.matcher(f.getName());
        if (m.matches()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        return Integer.MAX_VALUE;
    }

    private static void copyExactly(InputStream in, OutputStream out, long size) throws IOException {
        byte[] buf = new byte[8192];
        long remaining = size;
        while (remaining > 0) {
            int toRead = (int) Math.min(buf.length, remaining);
            int read = in.read(buf, 0, toRead);
            if (read == -1) throw new EOFException("unexpected EOF, need " + remaining + " more bytes");
            out.write(buf, 0, read);
            remaining -= read;
        }
    }

    private static void silentDelete(File dir) {
        if (!dir.exists()) return;
        File[] children = dir.listFiles();
        if (children != null) for (File c : children) silentDelete(c);
        //noinspection ResultOfMethodCallIgnored
        dir.delete();
    }
}
