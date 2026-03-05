package com.app.controller;

import java.io.IOException;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.service.IFileReaderService;

@RestController
@RequestMapping("/reader")
@Validated
public class FileReaderController {
	@Autowired
	IFileReaderService iFileReader;

	public FileReaderController() {
		System.out.println("in Constructor of " + getClass().getName());
	}

	@GetMapping("/fileread")
	public ResponseEntity<?> getFileData(Principal principal) throws IOException, Exception {
		iFileReader.fileReader(null);
		return new ResponseEntity<>(HttpStatus.OK);
	
	
	
	}
}
