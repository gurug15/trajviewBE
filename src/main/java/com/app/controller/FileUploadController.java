package com.app.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.app.service.IFetchIndexFileDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import com.app.message.ResponseMessage;
import com.app.pojos.FileInfo;
import com.app.service.IFilesStorageServiceImpl;

import jakarta.validation.Valid;

@RestController
//@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/fileUpload")
@Validated // To enable : validation err handling on request params or path variables

public class FileUploadController {
	@Autowired
	IFilesStorageServiceImpl storageService;

	@Autowired
	IFetchIndexFileDataService iFetchIndexFileData;

	public FileUploadController() {
		System.out.println("in Constructor of " + getClass().getName());
	}
	// HttpSession session ;

	// Multiple Files upload

	@PostMapping("/uploadMultipleFiles")
	public ResponseEntity<?> uploadFiles(@RequestParam("files") @Valid MultipartFile[] files,Principal principal) {
		System.out.println("in fileuploadController- Multiple Files upload ");
		String message = "";
		try {
//			storageService.deleteAll();
//			storageService.init();

//			String[] fileNames = new String[2];
			ArrayList<String> fileNames = new ArrayList<>();
			Arrays.asList(files).stream().forEach(file -> {
				storageService.save(file,principal.getName());
				fileNames.add(file.getOriginalFilename());

			});

			System.out.println(fileNames);

			message = "Uploaded the files successfully: " + fileNames;

			return ResponseEntity.status(HttpStatus.OK)
					.body(new Object[] { new ResponseMessage(fileNames.toString()) });

		} catch (Exception e) {
			message = "Fail to upload files!";
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
		}
	}

	@PostMapping("/uploadIndexFile")
	public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,Principal principal) {
		String message = "";
		try {
//			if (file.getOriginalFilename().contains(".sh")) {
//			storageService.deleteAll();
//			storageService.init();
//			}

			String filenameWithPath = storageService.save(file,principal.getName());
			System.out.println("in controller filenameWithPath ::" + filenameWithPath);

			message = "Uploaded the file successfully: " + file.getOriginalFilename();
			System.out.println("Filename::"+file.getOriginalFilename());
			int analysisWindowNumber=0;
			iFetchIndexFileData.fetchIndexDataGromacs(file.getOriginalFilename(),principal.getName(),analysisWindowNumber);
			return ResponseEntity.status(HttpStatus.OK).body(new Object[] { new ResponseMessage(filenameWithPath) });

		} catch (Exception e) {
			message = "Could not upload the file: " + file.getOriginalFilename() + "!";
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
		}
	}

	@PostMapping("/files")
	public ResponseEntity<List<FileInfo>> getListFiles(@RequestBody String analysisName, Principal principal) {
		//analysisName= /gromacs/rmsd, /gromacs/rmsf etc...
		List<FileInfo> fileInfos = storageService.loadAll(analysisName, principal.getName()).map(path -> {
			String filename = path.getFileName().toString();
//internally calling 	@GetMapping("/files/{filename:.+}")
			String url = MvcUriComponentsBuilder
					.fromMethodName(FileUploadController.class, "getFile",analysisName, path.getFileName().toString(),principal.getName()).build()
					.toString();
			return new FileInfo(filename, url);
		}).collect(Collectors.toList());

		return ResponseEntity.status(HttpStatus.OK).body(fileInfos);
	}

	@GetMapping("/files/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> getFile(String analysisName,@PathVariable String filename,String userName) {
		Resource file = storageService.load(analysisName,filename,userName);
		System.out.println("in get files::  " + filename);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
				.body(file);
	}
}
