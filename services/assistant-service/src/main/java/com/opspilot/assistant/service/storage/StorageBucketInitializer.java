package com.opspilot.assistant.service.storage;

import com.opspilot.assistant.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Ensures the MinIO document storage bucket exists at application startup.
 *
 * If {@code assistant.storage.auto-create-bucket} is {@code true} (the default) and the
 * configured bucket does not yet exist, this component creates it. This prevents ingestion
 * failures caused by a missing bucket in fresh local or CI environments.
 */
@Component
public class StorageBucketInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StorageBucketInitializer.class);

    private final S3Client s3Client;
    private final StorageProperties properties;

    public StorageBucketInitializer(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureBucket();
    }

    void ensureBucket() {
        if (!properties.isAutoCreateBucket()) {
            return;
        }

        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucket()).build());
        } catch (NoSuchBucketException ex) {
            createBucket();
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                createBucket();
            } else {
                throw new StorageException("Failed to initialize storage bucket", ex);
            }
        }
    }

    private void createBucket() {
        log.info("assistant_storage_bucket_create bucket={}", properties.getBucket());
        s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucket()).build());
    }
}
