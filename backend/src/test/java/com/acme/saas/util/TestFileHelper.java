package com.acme.saas.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Helper utility for loading test files from resources.
 */
public class TestFileHelper {

    private static final String TEST_FILES_PATH = "test-files/";

    /**
     * Get an InputStream for a test file.
     *
     * @param filename The name of the file in test-files/ directory
     * @return InputStream for the file
     * @throws IOException if file cannot be loaded
     */
    public static InputStream getTestFileStream(String filename) throws IOException {
        ClassPathResource resource = new ClassPathResource(TEST_FILES_PATH + filename);
        if (!resource.exists()) {
            throw new IOException("Test file not found: " + TEST_FILES_PATH + filename);
        }
        return resource.getInputStream();
    }

    /**
     * Get test file content as byte array.
     *
     * @param filename The name of the file in test-files/ directory
     * @return File content as bytes
     * @throws IOException if file cannot be loaded
     */
    public static byte[] getTestFileBytes(String filename) throws IOException {
        try (InputStream inputStream = getTestFileStream(filename)) {
            return inputStream.readAllBytes();
        }
    }

    /**
     * Create a MockMultipartFile from a test file.
     *
     * @param filename The name of the file in test-files/ directory
     * @param contentType The content type to set
     * @return MockMultipartFile for testing
     * @throws IOException if file cannot be loaded
     */
    public static MockMultipartFile createMockMultipartFile(String filename, String contentType) throws IOException {
        byte[] content = getTestFileBytes(filename);
        return new MockMultipartFile(
                "file",
                filename,
                contentType,
                content
        );
    }
}
