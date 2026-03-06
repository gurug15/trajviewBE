package com.app.service;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IFilesStorageServiceImpl {
	
	 void init();
	    
	    /**
	     * Save uploaded file to user's input directory
	     */
	    String save(MultipartFile file, String userName);
	    
	    /**
	     * Load a file from user's output directory (for analysis results)
	     */
	    Resource load(String analysisName, String fileName, String userName);
	    
	    /**
	     * Load all files from user's output directory
	     */
	    Stream<Path> loadAll(String analysisName, String userName);
	    
	    void deleteAll();
	
}
