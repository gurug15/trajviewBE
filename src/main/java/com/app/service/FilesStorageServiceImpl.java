package com.app.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import com.app.pojos.User;
import com.app.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FilesStorageServiceImpl implements IFilesStorageServiceImpl {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void init() {
        try {
            log.debug("in init folder create");
            // Directory creation is now handled per-user in save() method
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    @Override
    public String save(MultipartFile file, String userName) {
        try {
            User user = userRepository.findByEmail(userName)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Saving file for user: {}", userName);
            
            // Get input file path
            Path inputDirPath = Paths.get(user.getInputfilePath()).toAbsolutePath();
            
            // Create directory if it doesn't exist
            if (!Files.exists(inputDirPath)) {
                Files.createDirectories(inputDirPath);
                log.info("Created directory: {}", inputDirPath);
            }
            
            // Validate filename to prevent path traversal
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                throw new RuntimeException("Invalid filename");
            }
            
            if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
                throw new RuntimeException("Invalid filename: path traversal detected");
            }
            
            // Resolve file path
            Path filePath = inputDirPath.resolve(originalFilename);
            
            // Save file
            file.transferTo(filePath);
            
            log.info("File saved successfully: {}", filePath);
            
            return filePath.toString();
            
        } catch (Exception e) {
            log.error("Error saving file: {}", e.getMessage());
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    @Override
    public Resource load(String analysisName, String fileName, String userName) {
        try {
            User user = userRepository.findByEmail(userName)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Path for output files: analysis/{email}/{analysisName}/{fileName}
            Path file = Paths.get("analysis/" + user.getEmail() + analysisName).resolve(fileName);
            
            log.info("Loading file: {}", file);
            
            Resource resource = new UrlResource(file.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @Override
    public void deleteAll() {
        // This is dangerous - consider removing or making it admin-only
        log.warn("deleteAll() called - this will delete all files!");
        // Keeping original implementation but adding warning
        Path fileStorePath = Paths.get("analysis/nik@gmail.com/gromacs/inputfiles");
        FileSystemUtils.deleteRecursively(fileStorePath.toFile());
    }

    @Override
    public Stream<Path> loadAll(String analysisName, String userName) {
        try {
            User user = userRepository.findByEmail(userName)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Path outputdirPath = Paths.get("analysis/" + user.getEmail() + analysisName);
            
            log.info("Loading all files from: {}", outputdirPath);
            
            // Create directory if it doesn't exist
            if (!Files.exists(outputdirPath)) {
                Files.createDirectories(outputdirPath);
                return Stream.empty();
            }
            
            return Files.walk(outputdirPath, 1)
                    .filter(path -> !path.equals(outputdirPath))
                    .map(outputdirPath::relativize);
                    
        } catch (IOException e) {
            throw new RuntimeException("Could not load the files!");
        }
    }
}