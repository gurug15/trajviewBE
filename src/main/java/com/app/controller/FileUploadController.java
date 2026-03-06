package com.app.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import com.app.message.ResponseMessage;
import com.app.pojos.FileInfo;
import com.app.service.IFetchIndexFileDataService;
import com.app.service.IFileReaderService;
import com.app.service.IFilesStorageServiceImpl;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/fileUpload")
@Validated
public class FileUploadController {
    
    @Autowired
    private IFilesStorageServiceImpl storageService;

    @Autowired
    private IFetchIndexFileDataService iFetchIndexFileData;
    
    @Autowired
    private IFileReaderService fileReaderService;

    public FileUploadController() {
        System.out.println("in Constructor of " + getClass().getName());
    }

    /**
     * POST /fileUpload/inputfiles
     * Upload single file (topology, trajectory, or index)
     */
    @PostMapping("/inputfiles")
    public ResponseEntity<?> uploadSingleFile(
            @RequestParam("file") @Valid MultipartFile file,
            Principal principal) {
        
        System.out.println("in fileuploadController - Single File upload");
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ResponseMessage("File is empty"));
            }
            
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ResponseMessage("Invalid filename"));
            }
            
            // Optional: Validate file extensions
            String lowerFilename = originalFilename.toLowerCase();
            if (!lowerFilename.endsWith(".pdb") && 
                !lowerFilename.endsWith(".gro") && 
                !lowerFilename.endsWith(".xtc") && 
                !lowerFilename.endsWith(".ndx")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ResponseMessage("Invalid file type. Only .pdb, .gro, .xtc, .ndx files are allowed"));
            }
            
            // Save file
            String filenameWithPath = storageService.save(file, principal.getName());
            
            System.out.println("File saved at: " + filenameWithPath);
            
            String message = "Uploaded the file successfully: " + originalFilename;
            
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseMessage(message));
                    
        } catch (Exception e) {
            e.printStackTrace();
            String message = "Could not upload the file: " + file.getOriginalFilename();
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseMessage(message + " - Error: " + e.getMessage()));
        }
    }

    /**
     * POST /fileUpload/uploadMultipleFiles
     * Upload multiple files at once
     */
    @PostMapping("/uploadMultipleFiles")
    public ResponseEntity<?> uploadFiles(
            @RequestParam("files") @Valid MultipartFile[] files,
            Principal principal) {
        
        System.out.println("in fileuploadController - Multiple Files upload");
        
        try {
            ArrayList<String> fileNames = new ArrayList<>();
            ArrayList<String> failedFiles = new ArrayList<>();
            
            Arrays.asList(files).forEach(file -> {
                try {
                    storageService.save(file, principal.getName());
                    fileNames.add(file.getOriginalFilename());
                } catch (Exception e) {
                    failedFiles.add(file.getOriginalFilename());
                    System.err.println("Failed to upload: " + file.getOriginalFilename());
                }
            });

            System.out.println("Uploaded files: " + fileNames);
            
            if (!failedFiles.isEmpty()) {
                System.out.println("Failed files: " + failedFiles);
            }

            String message = "Uploaded " + fileNames.size() + " file(s) successfully";
            if (!failedFiles.isEmpty()) {
                message += ". Failed: " + failedFiles.size();
            }

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseMessage(message));

        } catch (Exception e) {
            String message = "Fail to upload files!";
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseMessage(message));
        }
    }

    /**
     * POST /fileUpload/uploadIndexFile
     * Upload index file with special processing
     */
    @PostMapping("/uploadIndexFile")
    public ResponseEntity<?> uploadIndexFile(
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        
        try {
            String filenameWithPath = storageService.save(file, principal.getName());
            System.out.println("Index file saved at: " + filenameWithPath);

            String message = "Uploaded the file successfully: " + file.getOriginalFilename();
            System.out.println("Filename: " + file.getOriginalFilename());
            
            // Process index file data
            int analysisWindowNumber = 0;
            iFetchIndexFileData.fetchIndexDataGromacs(
                    file.getOriginalFilename(), 
                    principal.getName(), 
                    analysisWindowNumber);
            
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseMessage(filenameWithPath));

        } catch (Exception e) {
            String message = "Could not upload the file: " + file.getOriginalFilename() + "!";
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseMessage(message));
        }
    }

    /**
     * POST /fileUpload/files
     * Get list of output files for a specific analysis
     */
    @PostMapping("/files")
    public ResponseEntity<List<FileInfo>> getListFiles(
            @RequestBody String analysisName, 
            Principal principal) {
        
        // analysisName = /gromacs/rmsd, /gromacs/rmsf etc...
        List<FileInfo> fileInfos = storageService.loadAll(analysisName, principal.getName())
                .map(path -> {
                    String filename = path.getFileName().toString();
                    String url = MvcUriComponentsBuilder
                            .fromMethodName(FileUploadController.class, 
                                    "getFile", 
                                    analysisName, 
                                    path.getFileName().toString(), 
                                    principal.getName())
                            .build()
                            .toString();
                    return new FileInfo(filename, url);
                })
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(fileInfos);
    }

    /**
     * GET /fileUpload/files/{filename}
     * Download output file
     */
    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> getFile(
            String analysisName,
            @PathVariable String filename,
            String userName) {
        
        Resource file = storageService.load(analysisName, filename, userName);
        System.out.println("Downloading file: " + filename);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }
}