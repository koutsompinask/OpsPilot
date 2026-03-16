package com.opspilot.assistant.service.storage;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@ExtendWith(MockitoExtension.class)
class DocumentStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Test
    void constructorShouldNotInitializeBucket() {
        StorageProperties properties = new StorageProperties();

        new DocumentStorageService(s3Client, properties);

        verify(s3Client, never()).headBucket(org.mockito.ArgumentMatchers.any(HeadBucketRequest.class));
        verify(s3Client, never()).createBucket(org.mockito.ArgumentMatchers.any(CreateBucketRequest.class));
    }
}
