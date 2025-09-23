package com.digitalarkcorp.filestorage.infrastructure.s3;

import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import com.digitalarkcorp.filestorage.infrastructure.config.StorageProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;

import java.io.InputStream;

public class S3StorageAdapter implements StoragePort {

    private final MinioClient client;
    private final StorageProperties props;

    public S3StorageAdapter(MinioClient client, StorageProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public void put(String objectKey, InputStream data, long contentLength, String contentType) {
        try {
            PutObjectArgs.Builder b = PutObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(objectKey)
                    .stream(data, contentLength, -1);
            if (contentType != null && !contentType.isBlank()) {
                b.contentType(contentType);
            }
            client.putObject(b.build());
        } catch (Exception e) {
            throw new RuntimeException("s3 put failed", e);
        }
    }

    @Override
    public Resource get(String objectKey) {
        try {
            StatObjectResponse stat = client.statObject(
                    StatObjectArgs.builder()
                            .bucket(props.bucket())
                            .object(objectKey)
                            .build()
            );
            InputStream in = client.getObject(
                    GetObjectArgs.builder()
                            .bucket(props.bucket())
                            .object(objectKey)
                            .build()
            );
            String ct = stat.contentType();
            long len = stat.size();
            return new Resource(in, len, ct);
        } catch (Exception e) {
            throw new RuntimeException("s3 get failed", e);
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
            throw new RuntimeException("s3 delete failed", e);
        }
    }
}
