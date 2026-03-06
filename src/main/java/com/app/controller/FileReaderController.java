package com.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.app.service.IFileReaderService;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
public class FileReaderController {
		
    @Autowired
    private IFileReaderService fileReaderService;
		
    /**
     * GET /topology/list
     * Returns topology files (.pdb, .gro)
     */
    @GetMapping("/topology/list")
    public ResponseEntity<List<String>> listTopologyFiles(Principal principal) {
        try {
            List<String> fileNames = fileReaderService.getFilesByExtensions(
                    principal.getName(), ".pdb", ".gro");
            System.out.println("Topology files found: " + fileNames); // Debug log
            return ResponseEntity.ok(fileNames);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }
    
    /**
     * GET /trajectory/list
     * Returns trajectory files (.xtc)
     */
    @GetMapping("/trajectory/list")
    public ResponseEntity<List<String>> listTrajectoryFiles(Principal principal) {
        try {
            List<String> fileNames = fileReaderService.getFilesByExtensions(
                    principal.getName(), ".xtc");
            System.out.println("Trajectory files found: " + fileNames); // Debug log
            return ResponseEntity.ok(fileNames);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }
    
    /**
     * GET /index/list
     * Returns index files (.ndx)
     */
    @GetMapping("/index/list")
    public ResponseEntity<List<String>> listIndexFiles(Principal principal) {
        try {
            List<String> fileNames = fileReaderService.getFilesByExtensions(
                    principal.getName(), ".ndx");
            System.out.println("Index files found: " + fileNames); // Debug log
            return ResponseEntity.ok(fileNames);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }
    
    /**
     * GET /topology/file/{filename}
     * GET /trajectory/file/{filename}
     * GET /index/file/{filename}
     * Streams the file content directly to the client
     */
    @GetMapping({
        "/topology/file/{filename:.+}",
        "/trajectory/file/{filename:.+}",
        "/index/file/{filename:.+}"
    })
    public ResponseEntity<Resource> getFile(
            @PathVariable("filename") String filename, 
            Principal principal) {
        try {
            System.out.println("Fetching file: " + filename + " for user: " + principal.getName()); // Debug log
            Resource resource = fileReaderService.getFile(filename, principal.getName());
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IllegalArgumentException e) {
            System.err.println("Bad request for file: " + filename + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (SecurityException e) {
            System.err.println("Forbidden access to file: " + filename + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IOException e) {
            System.err.println("File not found: " + filename + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    /**
     * DELETE /file/{filename}
     * Deletes any file by filename
     */
    @DeleteMapping("/file/{filename:.+}")
    public ResponseEntity<String> deleteFile(
            @PathVariable("filename") String filename, 
            Principal principal) {
        try {
            System.out.println("Deleting file: " + filename + " for user: " + principal.getName()); // Debug log
            fileReaderService.deleteFile(filename, principal.getName());
            return ResponseEntity.ok("File deleted successfully");
        } catch (IllegalArgumentException e) {
            System.err.println("File not found for deletion: " + filename + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("File not found: " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Access denied for deletion: " + filename + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error deleting file: " + filename + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting file: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error occurred");
        }
    }
}