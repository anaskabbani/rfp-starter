package com.acme.saas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path storageLocation;
    
    public FileStorageService(@Value("${app.storage.location:./storage}") String storageLocation) {
        this.storageLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }
    
    /**
     * Stores a file and returns the storage path.
     * The path is organized by tenant and uses UUID for uniqueness.
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
        
        // Organize by tenant: storage/tenant_<id>/<filename>
        Path tenantDir = this.storageLocation.resolve("tenant_" + tenantId.toLowerCase());
        Files.createDirectories(tenantDir);
        
        Path targetLocation = tenantDir.resolve(filename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        // Return relative path for storage (tenant_<id>/<filename>)
        return "tenant_" + tenantId.toLowerCase() + "/" + filename;
    }
    
    /**
     * Retrieves the file path for reading.
     */
    public Path getFilePath(String storagePath) {
        return this.storageLocation.resolve(storagePath).normalize();
    }
    
    /**
     * Deletes a file from storage.
     */
    public void deleteFile(String storagePath) throws IOException {
        Path filePath = getFilePath(storagePath);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }
    
    /**
     * Checks if a file exists.
     */
    public boolean fileExists(String storagePath) {
        return Files.exists(getFilePath(storagePath));
    }
}

