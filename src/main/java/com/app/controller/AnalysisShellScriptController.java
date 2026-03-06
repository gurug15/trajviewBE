package com.app.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.app.dto.TrajectoryFrameInput;
import com.app.pojos.GyrateGromacsUserInput;
import com.app.service.IGyrateGromacsAnalysisService;
import com.app.service.IStructureFromFrameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.app.pojos.RmsdGromacsUserInput;
import com.app.pojos.RmsfGromacsUserInput;
import com.app.service.IRmsdGromacsAnalysisService;
import com.app.service.IRmsfGromacsAnalysisService;

import jakarta.validation.Valid;

@RestController
// @CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/analysis")
@Validated // To enable : validation err handling on request params or path variables
public class AnalysisShellScriptController {
	@Autowired
	IRmsdGromacsAnalysisService rmsdGromacs;
	@Autowired
	IRmsfGromacsAnalysisService rmsfGromacs;
	@Autowired
	IGyrateGromacsAnalysisService gyrateGromacs;
	@Autowired
	IStructureFromFrameService structureFromFrameService;

	public AnalysisShellScriptController() {
		System.out.println("in Co3500 of " + getClass().getName());
	}

	// ========================
	// Structure from Frame (PDB extraction)
	// ========================

	@PostMapping("/structurefromframe")
	public ResponseEntity<?> structureFromFrame(@RequestBody @Valid TrajectoryFrameInput input, Principal principal) {
		try {
			// Use the authenticated user's email if userName not provided in body
			if (input.getUserName() == null || input.getUserName().isBlank()) {
				input.setUserName(principal.getName());
			}
			String outputFileName = structureFromFrameService.extractPdbFromFrame(input);
			return ResponseEntity.ok(Map.of("outputFileName", outputFileName));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Failed to extract PDB: " + e.getMessage()));
		}
	}

	// ========================
	// PDB Download (permitAll — accessed via window.location.href)
	// ========================

	@GetMapping("/download/pdb/{outputFileName}")
	public ResponseEntity<?> downloadPdb(
			@PathVariable String outputFileName,
			@RequestParam String userName) {
		try {
			Resource resource = structureFromFrameService.loadPdbFile(outputFileName, userName);
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=\"" + outputFileName + "\"")
					.body(resource);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("message", e.getMessage()));
		} catch (java.io.FileNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("message", "PDB file not found: " + outputFileName));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Failed to download PDB: " + e.getMessage()));
		}
	}

	// ========================
	// Single RMSD Analysis
	// ========================

	@PostMapping("/singleRmsdGromacs")
	public ResponseEntity<?> singleRmsdGromacs(@RequestBody @Valid RmsdGromacsUserInput gromacsRmsdUserInput,
			Principal principal)
			throws IOException, Exception {
		try {
			System.out.println("singleRmsdGromacs input:: " + gromacsRmsdUserInput.toString());
			List<Double[]> graphData = rmsdGromacs.writeRmsdAnalysisScript(gromacsRmsdUserInput, principal.getName(),
					0);
			return new ResponseEntity<>(graphData, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "RMSD analysis failed: " + e.getMessage()));
		}
	}

	// ========================
	// Single RMSF Analysis
	// ========================

	@PostMapping("/singleRmsfGromacs")
	public ResponseEntity<?> singleRmsfGromacs(@RequestBody @Valid RmsfGromacsUserInput rmsfGromacsUserInput,
			Principal principal)
			throws IOException, Exception {
		try {
			System.out.println("singleRmsfGromacs input:: " + rmsfGromacsUserInput.toString());
			List<Double[]> graphData = rmsfGromacs.writeRmsfAnalysisScript(rmsfGromacsUserInput, principal.getName(),
					0);
			return new ResponseEntity<>(graphData, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "RMSF analysis failed: " + e.getMessage()));
		}
	}

	// ========================
	// Existing Batch Endpoints
	// ========================

	// @RequestBody @valid GromacsRmsdUserInput gromacsRmsdUserInput
	@PostMapping("/rmsdGromacs")
	public ResponseEntity<?> rmsdGromacs(@RequestBody @Valid RmsdGromacsUserInput[] gromacsRmsdUserInput,
			Principal principal)
			throws IOException, Exception {
		// Thread.sleep(5000); // Simulating delay
		List<List<Double[]>> graphArray = new ArrayList<>();
		System.out.println("analysis size::" + gromacsRmsdUserInput.length);
		for (int i = 0; i < gromacsRmsdUserInput.length; i++) {
			graphArray.add(rmsdGromacs.writeRmsdAnalysisScript(gromacsRmsdUserInput[i], principal.getName(), i));
			System.out.println("user input[" + i + "] ::" + gromacsRmsdUserInput[i].toString());

		}
		// List<Double[]>
		// graphArray1=rmsdGromacs.write(gromacsRmsdUserInput[1],principal.getName());

		// return ResponseEntity.ok(gromacsRmsdUserInput);
		return new ResponseEntity<>(graphArray, HttpStatus.OK);
	}

	@PostMapping("/rmsfGromacs")
	public ResponseEntity<?> rmsfGromacs(@RequestBody @Valid RmsfGromacsUserInput[] rmsfGromacsUserInput,
			Principal principal)
			throws IOException, Exception {
		System.out.println("rmsfGromacsUserInput[0]::" + rmsfGromacsUserInput[0]);

		// Thread.sleep(5000); // Simulating delay
		List<List<Double[]>> graphArray = new ArrayList<>();
		System.out.println("analysis size::" + rmsfGromacsUserInput.length);
		for (int i = 0; i < rmsfGromacsUserInput.length; i++) {
			graphArray.add(rmsfGromacs.writeRmsfAnalysisScript(rmsfGromacsUserInput[i], principal.getName(), i));
			System.out.println("user input[" + i + "] ::" + rmsfGromacsUserInput[i].toString());
		}
		return new ResponseEntity<>(graphArray, HttpStatus.OK);
	}

	@PostMapping("/gyrateGromacs")
	public ResponseEntity<?> gyrateGromacs(@RequestBody @Valid GyrateGromacsUserInput[] gyrateGromacsUserInput,
			Principal principal)
			throws IOException, Exception {
		System.out.println("gyrateGromacsUserInput[0]::" + gyrateGromacsUserInput[0]);

		// Thread.sleep(5000); // Simulating delay
		List<List<Double[]>> graphArray = new ArrayList<>();
		System.out.println("analysis size::" + gyrateGromacsUserInput.length);
		for (int i = 0; i < gyrateGromacsUserInput.length; i++) {
			graphArray.add(gyrateGromacs.writeGyrateAnalysisScript(gyrateGromacsUserInput[i], principal.getName(), i));
			System.out.println("user input[" + i + "] ::" + gyrateGromacsUserInput[i].toString());
		}
		return new ResponseEntity<>(graphArray, HttpStatus.OK);
	}
}
