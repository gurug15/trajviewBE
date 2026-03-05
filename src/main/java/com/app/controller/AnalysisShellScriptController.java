package com.app.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import com.app.pojos.GyrateGromacsUserInput;
import com.app.service.IGyrateGromacsAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.pojos.RmsdGromacsUserInput;
import com.app.pojos.RmsfGromacsUserInput;
import com.app.service.IRmsdGromacsAnalysisService;
import com.app.service.IRmsfGromacsAnalysisService;

import jakarta.validation.Valid;

@RestController
//@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/analysis")
@Validated // To enable : validation err handling on request params or path variables
public class AnalysisShellScriptController {	
	@Autowired
	IRmsdGromacsAnalysisService rmsdGromacs;
	@Autowired
	IRmsfGromacsAnalysisService rmsfGromacs;
	@Autowired
	IGyrateGromacsAnalysisService gyrateGromacs;

	public AnalysisShellScriptController() {		
		System.out.println("in Co3500 of " + getClass().getName());
	}

//	@RequestBody @valid GromacsRmsdUserInput gromacsRmsdUserInput
	@PostMapping("/rmsdGromacs")
	public ResponseEntity<?> rmsdGromacs(@RequestBody @Valid RmsdGromacsUserInput[] gromacsRmsdUserInput, Principal principal)
			throws IOException, Exception {
//        Thread.sleep(5000); // Simulating delay
		List<List<Double[]>> graphArray = new ArrayList<>();
		System.out.println("analysis size::"+gromacsRmsdUserInput.length);
		for (int i = 0; i < gromacsRmsdUserInput.length; i++) {
			graphArray.add(rmsdGromacs.writeRmsdAnalysisScript(gromacsRmsdUserInput[i],principal.getName(),i));
			System.out.println("user input["+i+"] ::" + gromacsRmsdUserInput[i].toString());

		}
//		List<Double[]> graphArray1=rmsdGromacs.write(gromacsRmsdUserInput[1],principal.getName());

//			return ResponseEntity.ok(gromacsRmsdUserInput);
		return new ResponseEntity<>(graphArray, HttpStatus.OK);
	}
	
	@PostMapping("/rmsfGromacs")
	public ResponseEntity<?> rmsfGromacs(@RequestBody @Valid RmsfGromacsUserInput[] rmsfGromacsUserInput, Principal principal)
			throws IOException, Exception {
		System.out.println("rmsfGromacsUserInput[0]::"+rmsfGromacsUserInput[0]);

//        Thread.sleep(5000); // Simulating delay
		List<List<Double[]>> graphArray = new ArrayList<>();
		System.out.println("analysis size::"+rmsfGromacsUserInput.length);
		for (int i = 0; i < rmsfGromacsUserInput.length; i++) {
			graphArray.add(rmsfGromacs.writeRmsfAnalysisScript(rmsfGromacsUserInput[i],principal.getName(),i));
			System.out.println("user input["+i+"] ::" + rmsfGromacsUserInput[i].toString());
		}
		return new ResponseEntity<>(graphArray, HttpStatus.OK);
	}

	@PostMapping("/gyrateGromacs")
	public ResponseEntity<?> gyrateGromacs(@RequestBody @Valid GyrateGromacsUserInput[] gyrateGromacsUserInput, Principal principal)
			throws IOException, Exception {
		System.out.println("gyrateGromacsUserInput[0]::"+gyrateGromacsUserInput[0]);

//        Thread.sleep(5000); // Simulating delay
		List<List<Double[]>> graphArray = new ArrayList<>();
		System.out.println("analysis size::"+gyrateGromacsUserInput.length);
		for (int i = 0; i < gyrateGromacsUserInput.length; i++) {
			graphArray.add(gyrateGromacs.writeGyrateAnalysisScript(gyrateGromacsUserInput[i],principal.getName(),i));
			System.out.println("user input["+i+"] ::" + gyrateGromacsUserInput[i].toString());
		}
		return new ResponseEntity<>(graphArray, HttpStatus.OK);
	}
}
