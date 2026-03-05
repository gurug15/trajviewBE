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
//	@Value("${file.upload.location}")
//	private String uploadLocation;

	@Autowired
	UserRepository userRepository;
//	

	// use only to get or display output files on frontend
//	private final Path outputdirPath = Paths.get("analysis/rmsdGromacs/ouputFiles");
//	@Autowired
//    private HttpSession session;
//	@Value("${variables.fileStoreDirPath}")
//	String fileStoreDirPath;
//
//	
//	@Value("${variables.outputdir}")
//	String outputdir;
//	
//	private final Path fileStorePath = Paths.get(fileStoreDirPath);
//	private final Path outputdirPath = Paths.get(outputdir);

	@Override
	public void init() {
		try {
			log.debug("in init folder create");

//			Files.createDirectory(fileStorePath);
//			Files.createDirectory(outputdirPath);

		} catch (Exception e) {
			throw new RuntimeException("Could not initialize folder for upload!");
		}
	}

	@Override
	public String save(MultipartFile file, String userName) {

		try {
			User user = userRepository.findByEmail(userName).orElseThrow(() -> new RuntimeException("User not Found"));
//	        session.setAttribute("outputdir", user.getOutputDirPath() + "/gromacs/rmsd/");

			System.out.println("in fileStorageservice Save method");

			String filepath = (Paths.get(user.getInputfilePath()).resolve(file.getOriginalFilename())).toString();

			// java.io.File destinationPath= new java.io.File(filepath);
			file.transferTo(Paths.get(user.getInputfilePath()).resolve(file.getOriginalFilename()));

			return filepath;

		} catch (Exception e) {
			throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
		}
	}

	@Override
	public Resource load(String analysisName,String fileName,String userName) {
//		System.out.println("analysisName::" + analysisName);
//analysisName= /gromacs/rmsd,/gromacs/rmsf etc...
		try {
			User user = userRepository.findByEmail(userName).orElseThrow(() -> new RuntimeException("User not Found"));

			Path file = Paths.get("analysis/"+user.getEmail() +analysisName).resolve(fileName);
			System.out.println("File::" + file);
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
		Path fileStorePath = Paths.get("analysis/nik@gmail.com/gromacs/inputfiles");
		FileSystemUtils.deleteRecursively(fileStorePath.toFile());
	}

	@Override
	public Stream<Path> loadAll(String analysisName,String userName) {
		try {
			
			User user = userRepository.findByEmail(userName).orElseThrow(() -> new RuntimeException("User not Found"));
			Path outputdirPath = Paths.get("analysis/"+user.getEmail()+analysisName);
			System.out.println("outputdirPath::" + outputdirPath);
			return Files.walk(outputdirPath, 1).filter(path -> !path.equals(outputdirPath))
					.map(outputdirPath::relativize);
		} catch (IOException e) {
			throw new RuntimeException("Could not load the files!");
		}
	}
}
