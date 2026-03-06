package com.app.service;

import com.app.pojos.User;
import com.app.repository.UserRepository;
import com.app.service.IFileReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileReaderServiceImpl implements IFileReaderService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public List<String> getFilesByExtensions(String username, String... extensions) throws IOException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Path inputDirPath = Paths.get(user.getInputfilePath()).toAbsolutePath();
        
        // Create directory if it doesn't exist
        if (!Files.exists(inputDirPath)) {
            Files.createDirectories(inputDirPath);
            return new ArrayList<>();
        }
        
        try (Stream<Path> paths = Files.list(inputDirPath)) {
            List<String> allFiles = paths
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .sorted()
                    .collect(Collectors.toList());
            
            // If no extensions specified, return all files
            if (extensions == null || extensions.length == 0) {
                return allFiles;
            }
            
            // Filter by extensions (case-insensitive)
            List<String> lowerExtensions = Arrays.stream(extensions)
                    .map(ext -> ext.startsWith(".") ? ext.toLowerCase() : "." + ext.toLowerCase())
                    .collect(Collectors.toList());
            
            return allFiles.stream()
                    .filter(filename -> {
                        String lowerFilename = filename.toLowerCase();
                        return lowerExtensions.stream()
                                .anyMatch(lowerFilename::endsWith);
                    })
                    .collect(Collectors.toList());
                    
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("Failed to list files: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Resource getFile(String filename, String username) throws IOException {
        // Validate filename to prevent path traversal
        validateFilename(filename);
        
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Path inputDirPath = Paths.get(user.getInputfilePath()).toAbsolutePath();
        Path filePath = inputDirPath.resolve(filename).normalize();
        
        // Security check: ensure file is within user's directory
        if (!filePath.startsWith(inputDirPath)) {
            throw new SecurityException("Access denied: File is outside user directory");
        }
        
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filename);
        }
        
        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("Not a valid file");
        }
        
        return new FileSystemResource(filePath);
    }
    
    @Override
    public void deleteFile(String filename, String username) throws IOException {
        // Validate filename
        validateFilename(filename);
        
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Path inputDirPath = Paths.get(user.getInputfilePath()).toAbsolutePath();
        Path filePath = inputDirPath.resolve(filename).normalize();
        
        // Security check
        if (!filePath.startsWith(inputDirPath)) {
            throw new SecurityException("Access denied: File is outside user directory");
        }
        
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File not found: " + filename);
        }
        
        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("Not a valid file");
        }
        
        try {
            Files.delete(filePath);
        } catch (IOException e) {
            throw new IOException("Failed to delete file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates filename to prevent path traversal attacks
     */
    private void validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }
        
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename: path traversal detected");
        }
    }
}