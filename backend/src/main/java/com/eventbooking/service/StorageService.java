package com.eventbooking.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Centralsed storage service abstraction targeting local directory uploads.
 */
@Service
public class StorageService {

    private final Path root = Paths.get("uploads");

    public String store(String folder, MultipartFile file, String prefix) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file.");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = prefix + "_" + UUID.randomUUID() + ext;
        Path targetDir = root.resolve(folder);
        Files.createDirectories(targetDir);
        Files.copy(file.getInputStream(), targetDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + folder + "/" + filename;
    }
}
