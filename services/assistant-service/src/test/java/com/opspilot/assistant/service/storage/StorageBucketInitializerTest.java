package com.opspilot.assistant.service.storage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opspilot.assistant.exception.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class StorageBucketInitializerTest {

    @Mock
    private S3Client s3Client;

    @Test
    void runShouldCreateBucketWhenMissing() throws Exception {
        StorageProperties properties = new StorageProperties();
        StorageBucketInitializer initializer = new StorageBucketInitializer(s3Client, properties);
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(NoSuchBucketException.builder().message("missing").build());

        initializer.run(null);

        verify(s3Client).headBucket(any(HeadBucketRequest.class));
        verify(s3Client).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void runShouldSkipInitializationWhenAutoCreateDisabled() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setAutoCreateBucket(false);
        StorageBucketInitializer initializer = new StorageBucketInitializer(s3Client, properties);

        initializer.run(null);

        verify(s3Client, never()).headBucket(any(HeadBucketRequest.class));
        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void runShouldWrapUnexpectedStorageErrors() {
        StorageProperties properties = new StorageProperties();
        StorageBucketInitializer initializer = new StorageBucketInitializer(s3Client, properties);
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(S3Exception.builder().statusCode(500).message("boom").build());

        assertThatThrownBy(() -> initializer.run(null))
                .isInstanceOf(StorageException.class)
                .hasMessage("Failed to initialize storage bucket");
    }
}
