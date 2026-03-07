package com.opspilot.knowledgebase.service.storage;

import com.opspilot.knowledgebase.exception.StorageException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

    private final S3Client s3Client;
    private final StorageProperties properties;

    public DocumentStorageService(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
        ensureBucket();
    }

    public String store(UUID tenantId, UUID documentId, MultipartFile file) {
        String key = tenantId + "/" + documentId + "/" + sanitize(file.getOriginalFilename());
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return key;
        } catch (IOException | S3Exception ex) {
            throw new StorageException("Failed to store document", ex);
        }
    }

    public String loadText(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();
            byte[] content = s3Client.getObjectAsBytes(request).asByteArray();
            return new String(content, StandardCharsets.UTF_8);
        } catch (S3Exception ex) {
            throw new StorageException("Failed to read stored document", ex);
        }
    }

    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
        } catch (S3Exception ex) {
            throw new StorageException("Failed to delete document from storage", ex);
        }
    }

    private void ensureBucket() {
        if (!properties.isAutoCreateBucket()) {
            return;
        }

        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucket()).build());
        } catch (NoSuchBucketException ex) {
            log.info("knowledge_storage_bucket_create bucket={}", properties.getBucket());
            s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucket()).build());
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                log.info("knowledge_storage_bucket_create bucket={}", properties.getBucket());
                s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucket()).build());
            } else {
                throw new StorageException("Failed to initialize storage bucket", ex);
            }
        }
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document.txt";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
