package com.app.service;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IFilesStorageServiceImpl {
	
	public void init();

	public String save(MultipartFile file,String userName);

	public Resource load(String analysisName,String fileName,String userName);

	public void deleteAll();
	
//	public String getInputFilepath() ;

	public Stream<Path> loadAll(String analysisName,String userName);
	
	//public String getInputFilelocationPath() ;

	
}
