package com.app.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.core.io.Resource;


public interface IFileReaderService {
	 /**
     * Get files filtered by extensions from user's input directory
     */
    List<String> getFilesByExtensions(String username, String... extensions) throws IOException;
    
    /**
     * Get a specific file as Resource
     */
    Resource getFile(String filename, String username) throws IOException;
    
    /**
     * Delete a file by filename
     */
    void deleteFile(String filename, String username) throws IOException;
}


