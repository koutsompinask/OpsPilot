package com.opspilot.assistant.service.storage;

import com.opspilot.assistant.exception.StorageException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Manages raw document file storage in MinIO (S3-compatible object storage).
 *
 * Files are stored under the key pattern {@code {tenantId}/{documentId}/{sanitizedFilename}},
 * providing implicit tenant namespacing at the storage layer. This class handles upload,
 * full-content retrieval (for ingestion), and deletion.
 */
@Service
public class DocumentStorageService {

    private final S3Client s3Client;
    private final StorageProperties properties;

    public DocumentStorageService(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    /**
     * Uploads a document file to object storage and returns its storage key.
     *
     * The key follows the pattern {@code {tenantId}/{documentId}/{sanitizedFilename}}.
     *
     * @param tenantId   the owning tenant's ID (used as the top-level key prefix)
     * @param documentId the document's ID (used as the second-level key prefix)
     * @param file       the multipart file to upload
     * @return the storage key under which the file was persisted
     * @throws StorageException if the upload fails
     */
    public String store(UUID tenantId, UUID documentId, MultipartFile file) {
        // Key pattern: {tenantId}/{documentId}/{sanitizedFilename} — provides tenant namespacing in the shared bucket
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

    /**
     * Downloads the full content of a stored document and decodes it as UTF-8 text.
     *
     * @param key the storage key returned by {@link #store}
     * @return the document content as a string
     * @throws StorageException if the file cannot be retrieved
     */
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

    /**
     * Deletes a stored document file from object storage.
     *
     * @param key the storage key to delete
     * @throws StorageException if the deletion fails
     */
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

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document.txt";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
