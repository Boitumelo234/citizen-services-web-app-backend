package com.webapp.citizen_services_web_app_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.File;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get the absolute path to the uploads directory
        String userDir = System.getProperty("user.dir");
        String uploadPath = userDir + File.separator + "uploads" + File.separator;

        // Also try the relative path
        String relativePath = "uploads/";

        System.out.println("=== File Upload Configuration ===");
        System.out.println("User directory: " + userDir);
        System.out.println("Absolute upload path: " + uploadPath);
        System.out.println("Relative upload path: " + relativePath);
        System.out.println("File exists: " + new File(uploadPath).exists());
        System.out.println("================================");

        // Create directory if it doesn't exist
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
            System.out.println("Created uploads directory at: " + uploadPath);
        }

        // Serve files from uploads directory
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath, "file:" + relativePath)
                .setCachePeriod(0); // Disable cache for testing
    }
}