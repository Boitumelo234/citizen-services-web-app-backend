package com.webapp.citizen_services_web_app_backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootLocation;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            System.out.println("File storage initialized at: " + rootLocation);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize upload directory", exception);
        }
    }

    public String storeFile(MultipartFile file, Long complaintId) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        String filename = "complaint-" + complaintId + "-" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path destination = rootLocation.resolve(filename).normalize();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File saved: " + destination);
            System.out.println("File size: " + Files.size(destination) + " bytes");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store uploaded file", exception);
        }

        return "/uploads/" + filename;
    }

    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + filename, e);
        }
    }

    public Path resolveStoredFile(String filename) {
        return rootLocation.resolve(filename).normalize();
    }
}