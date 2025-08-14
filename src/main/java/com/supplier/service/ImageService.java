package com.supplier.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Service
@Slf4j
public class ImageService {

    @Value("${upload.dir}")
    private String uploadDir;

    /**
     * Gets the raw bytes of an image file
     * @param imagePath relative path to the image (e.g., "/uploads/filename.jpg")
     * @return raw image bytes
     */
    public byte[] getImageBytes(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return null;
        }

        try {
            // Handle both relative paths starting with /uploads/ and absolute paths
            Path filePath;
            if (imagePath.startsWith("/uploads/")) {
                // Remove the /uploads/ prefix and resolve against upload directory
                String filename = imagePath.substring("/uploads/".length());
                filePath = Paths.get(uploadDir, filename);
            } else if (imagePath.startsWith("http")) {
                // Skip external URLs - cannot convert to bytes
                log.warn("Cannot convert external URL to bytes: {}", imagePath);
                return null;
            } else {
                // Try as absolute path
                filePath = Paths.get(imagePath);
            }

            if (!Files.exists(filePath)) {
                log.warn("Image file not found: {}", filePath);
                return null;
            }

            return Files.readAllBytes(filePath);

        } catch (IOException e) {
            log.error("Error reading image file: {}", imagePath, e);
            return null;
        }
    }

    /**
     * Converts an image file to base64 encoded string
     * @param imagePath relative path to the image (e.g., "/uploads/filename.jpg")
     * @return base64 encoded image data
     */
    public String convertImageToBase64(String imagePath) {
        byte[] imageBytes = getImageBytes(imagePath);
        if (imageBytes != null) {
            return Base64.getEncoder().encodeToString(imageBytes);
        }
        return null;
    }

    /**
     * Extracts the file format/extension from an image path
     * @param imagePath path to the image
     * @return file format (e.g., "jpg", "png", "gif")
     */
    public String getImageFormat(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return null;
        }

        int lastDotIndex = imagePath.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < imagePath.length() - 1) {
            return imagePath.substring(lastDotIndex + 1).toLowerCase();
        }

        return "jpg"; // default format
    }
}
