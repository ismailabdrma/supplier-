package com.supplier;

import com.supplier.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = "upload.dir=uploads/")
public class ImageSoapTest {

    @Autowired
    private ImageService imageService;

    @Test
    public void testImageToBase64Conversion() {
        // Test with an actual image file from uploads directory
        String testImagePath = "/uploads/f0acd485-24c8-466d-8a36-4893a13c77df_asus-e410m-intel-celeron-n4020-4gb-128gb-bleu-90nb0q11-m15070.jpg";
        
        // Check if the file exists
        Path filePath = Paths.get("uploads", "f0acd485-24c8-466d-8a36-4893a13c77df_asus-e410m-intel-celeron-n4020-4gb-128gb-bleu-90nb0q11-m15070.jpg");
        if (Files.exists(filePath)) {
            // Test getting raw bytes
            byte[] imageBytes = imageService.getImageBytes(testImagePath);
            assertNotNull(imageBytes, "Image bytes should not be null");
            assertTrue(imageBytes.length > 0, "Image bytes should not be empty");
            
            // Test getting base64 string
            String base64Image = imageService.convertImageToBase64(testImagePath);
            assertNotNull(base64Image, "Base64 string should not be null");
            assertTrue(base64Image.length() > 0, "Base64 string should not be empty");
            
            // Test getting image format
            String format = imageService.getImageFormat(testImagePath);
            assertEquals("jpg", format, "Should detect jpg format");
            
            System.out.println("✅ Image processing test passed!");
            System.out.println("Image size: " + imageBytes.length + " bytes");
            System.out.println("Base64 length: " + base64Image.length() + " characters");
            System.out.println("Format: " + format);
        } else {
            System.out.println("⚠️  Test image not found, skipping test");
        }
    }

    @Test
    public void testExternalUrlHandling() {
        // Test with external URL - should return null
        String externalUrl = "https://images.unsplash.com/photo-1560472354-b33ff0c44a43?w=400";
        
        byte[] imageBytes = imageService.getImageBytes(externalUrl);
        assertNull(imageBytes, "External URL should return null for bytes");
        
        String base64Image = imageService.convertImageToBase64(externalUrl);
        assertNull(base64Image, "External URL should return null for base64");
        
        String format = imageService.getImageFormat(externalUrl);
        assertEquals("jpg", format, "Should still extract format from URL");
        
        System.out.println("✅ External URL handling test passed!");
    }
}

