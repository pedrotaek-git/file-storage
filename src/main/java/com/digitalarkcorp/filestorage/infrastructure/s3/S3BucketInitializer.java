package com.digitalarkcorp.filestorage.infrastructure.s3;

import com.digitalarkcorp.filestorage.infrastructure.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3", matchIfMissing = false)
public class S3BucketInitializer implements ApplicationRunner {

    private final MinioClient client;
    private final StorageProperties props;

    @Override
    public void run(ApplicationArguments args) {
        ensureBucket();
    }

    private void ensureBucket() {
        try {
            String bucket = props.getBucket();
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to ensure S3 bucket exists", e);
        }
    }
}
