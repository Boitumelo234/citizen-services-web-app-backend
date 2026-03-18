package com.webapp.citizen_services_web_app_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get the absolute path to the uploads directory
        String currentDir = System.getProperty("user.dir");
        String uploadPath = currentDir + "/uploads/";

        // Also try the project root
        String projectRoot = Paths.get(".").toAbsolutePath().normalize().toString();
        String altUploadPath = projectRoot + "/uploads/";

        System.out.println("Current directory: " + currentDir);
        System.out.println("Upload path 1: " + uploadPath);
        System.out.println("Upload path 2: " + altUploadPath);

        // Serve files from multiple possible locations
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                        "file:" + uploadPath,
                        "file:" + altUploadPath,
                        "file:./uploads/",
                        "file:uploads/"
                )
                .setCachePeriod(3600);

        // Also serve from a direct API endpoint
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations(
                        "file:" + uploadPath,
                        "file:" + altUploadPath,
                        "file:./uploads/",
                        "file:uploads/"
                )
                .setCachePeriod(3600);
    }
}