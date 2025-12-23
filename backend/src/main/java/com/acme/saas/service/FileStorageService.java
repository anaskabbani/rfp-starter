package com.acme.saas.service;

import com.acme.saas.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class FileStorageService {
    private final S3Client s3Client;
    private final String bucketName;

    public FileStorageService(S3Client s3Client,
                              @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /**
     * Stores a file in S3 and returns the S3 key.
     * The key is organized by tenant and uses UUID for uniqueness.
     */
    public String storeFile(MultipartFile file, String tenantId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "file";
        }

        String extension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFilename.substring(lastDot);
        }

        String filename = UUID.randomUUID() + extension;

        // Generate S3 key: tenant_<id>/<filename>
        String s3Key = "tenant_" + tenantId.toLowerCase() + "/" + filename;

        try {
            // Upload to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest,
                             RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return s3Key;
        } catch (S3Exception e) {
            throw new FileStorageException("Failed to upload file to S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Deletes a file from S3.
     */
    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            throw new FileStorageException("Failed to delete file from S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Checks if a file exists in S3.
     */
    public boolean fileExists(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            throw new FileStorageException("Failed to check file existence in S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Downloads a file from S3 and returns an InputStream.
     */
    public InputStream downloadFile(String s3Key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            return s3Client.getObject(getObjectRequest);
        } catch (S3Exception e) {
            throw new FileStorageException("Failed to download file from S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }
}
