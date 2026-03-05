package com.app.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.dto.JwtRequest;
import com.app.dto.JwtResponse;
import com.app.pojos.User;
import com.app.security.JwtHelper;
import com.app.service.UserServiceImpl;


@RestController
@RequestMapping("/auth")
//@CrossOrigin(origins = "*", allowedHeaders = "*")

public class  	AuthController {
	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private AuthenticationManager manager;

	@Autowired
	private JwtHelper helper;

	@Value("${variables.pythonExecutable}")
	String pythonExecutable;
	@Value("${variables.pythonScriptPath}")
	String pythonScriptPath;



	@Autowired
	private UserServiceImpl userServiceImpl;

	@PostMapping("/login")
	public ResponseEntity<JwtResponse> login(@RequestBody JwtRequest request) {
		System.out.println("in login");
		this.doAuthenticate(request.getEmail(), request.getPassword());

		UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
		String token = this.helper.generateToken(userDetails);

		JwtResponse response = JwtResponse.builder().jwtToken(token).username(userDetails.getUsername()).build();

//		User user = userRepository.findByEmail(request.getEmail())
//				.orElseThrow(() -> new EntityNotFoundException("Entity with id " + request.getEmail() + " not found"));
//
//		user.setInputfilePath("userdata/" + request.getEmail() + "/analysis/gromacs/inputfiles");
//		user.setOutputDirPath("userdata/" + request.getEmail() + "/analysis/");
//		userRepository.save(user);

/*
////pythonScriptcall

try {
	ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, pythonScriptPath);
	Process process = processBuilder.start();

	// Get input streams
	BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

	// Capture output
	StringBuilder output = new StringBuilder();
	String line;
	while ((line = reader.readLine()) != null) {
		output.append(line).append("\n");
	}

	// Capture errors
	StringBuilder errorOutput = new StringBuilder();
	while ((line = errorReader.readLine()) != null) {
		errorOutput.append(line).append("\n");
	}

	// Wait for the process to complete
	int exitCode = process.waitFor();

	// Check for errors
	if (exitCode == 0) {
		System.out.println("Script Output:");
		System.out.println(output.toString());
	} else {
		System.err.println("Error Output:");
		System.err.println(errorOutput.toString());
	}
}
catch (Exception e){
	e.printStackTrace();
}
*/
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	private void doAuthenticate(String email, String password) {

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email, password);
		try {
			manager.authenticate(authentication);

		} catch (Exception e) {
			throw new BadCredentialsException(" Invalid Username or Password  !!");
		}

	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<?> exceptionHandler() {
		return new ResponseEntity<>("Credentials Invalid !!", HttpStatus.UNAUTHORIZED);

//		return "Credentials Invalid !!";
	}

	@PostMapping("/create-user")
	public User createUser(@RequestBody User user) throws IOException {
		return userServiceImpl.createUser(user);
	}
}
