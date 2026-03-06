package com.app.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.app.pojos.RmsdGromacsUserInput;
import com.app.pojos.User;
import com.app.repository.UserRepository;

@Service
public class RmsdGromacsAnalysisServiceImpl implements IRmsdGromacsAnalysisService {

	@Value("${variables.gmxpath}")
	String gmxPath;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	IXVGGraphReaderService ixvgGraphReader;
	@Autowired
	IFileReaderService iFileReader;

	
	/**
	 * Check if a string is not null and not empty
	 */
	private boolean isNotEmpty(String str) {
	    return str != null && !str.trim().isEmpty();
	}

	private boolean isValidGroup(int groupIndex) {
	    return groupIndex >= 0;
	}
	/**
	 * Check if an integer is valid (not null and greater than 0)
	 */
	private boolean isValidFrame(Integer frameNo) {
	    return frameNo != null && frameNo >= 0;
	}
	
//	private final Path fileStorePath = Paths.get("uploads/");
//	private final Path analysisOutputDir = Paths.get("analysis/rmsdGromacs");

//	@Value("${variables.analysisOutputDirPath}")
//	String analysisOutputDirPath;
//	
//	@Value("${variables.fileStoreDirPath}")
//	String fileStoreDirPath;
//	
//	private final Path fileStorePath = Paths.get(fileStoreDirPath);
//	private final Path analysisOutputDir = Paths.get(analysisOutputDirPath);
	@Override
	public List<Double[]> writeRmsdAnalysisScript(RmsdGromacsUserInput rmsdGromacsUserInput, String username, int analysisWindowNumber) throws IOException {
	    User user = userRepository.findByEmail(username)
	            .orElseThrow(() -> new RuntimeException("User not Found"));
	    
	    String gromacsrmsdScriptPath = user.getOutputDirPath() + "/gromacs/rmsd/rmsdGromacsScript" + (analysisWindowNumber + 1) + ".sh";
	    Path inputDirPath = Paths.get(user.getInputfilePath()).toAbsolutePath();
	    Path outputDirPath = Paths.get(user.getOutputDirPath()).toAbsolutePath();
	    
	    // Ensure output directory exists
	    Files.createDirectories(Paths.get(user.getOutputDirPath() + "/gromacs/rmsd/"));
	    
	    try (FileWriter writer1 = new FileWriter(gromacsrmsdScriptPath);
	            BufferedWriter buffer = new BufferedWriter(writer1)) {
	        
	        // Build GROMACS command
	        StringBuilder cmd = new StringBuilder();
	        cmd.append(gmxPath).append(" rms")
	           .append(" -f ").append(inputDirPath).append("/").append(rmsdGromacsUserInput.getTrajectoryFileName())
	           .append(" -s ").append(inputDirPath).append("/").append(rmsdGromacsUserInput.getTopologyFileName());
	        
	        // Optional: Index file
	        if (isNotEmpty(rmsdGromacsUserInput.getIndexFileName())) {
	            cmd.append(" -n ").append(inputDirPath).append("/").append(rmsdGromacsUserInput.getIndexFileName());
	        }
	        
	        // Output file (mandatory)
	        cmd.append(" -o ").append(outputDirPath).append("/gromacs/rmsd/")
	           .append(rmsdGromacsUserInput.getOutputfileName()).append(".xvg");
	        
	        // Optional: First frame (only if provided and valid)
	        if (isValidFrame(rmsdGromacsUserInput.getFirstFrameno())) {
	            cmd.append(" -b ").append(rmsdGromacsUserInput.getFirstFrameno());
	        }
	        
	        // Optional: Last frame (only if provided and valid)
	        if (isValidFrame(rmsdGromacsUserInput.getLastFrameNo())) {
	            cmd.append(" -e ").append(rmsdGromacsUserInput.getLastFrameNo());
	        }
	        
	        // Optional: Interactive group selections (only if groups are provided)
	        boolean hasGroups = isValidGroup(rmsdGromacsUserInput.getGrouplsfit()) 
	                         || isValidGroup(rmsdGromacsUserInput.getGroupRMSD());
	        
	        if (hasGroups) {
	            cmd.append(" << EOF\n");
	            if (isValidGroup(rmsdGromacsUserInput.getGrouplsfit())) {
	                cmd.append(rmsdGromacsUserInput.getGrouplsfit()).append("\n");
	            }
	            if (isValidGroup(rmsdGromacsUserInput.getGroupRMSD())) {
	                cmd.append(rmsdGromacsUserInput.getGroupRMSD()).append("\n");
	            }
	            cmd.append("EOF");
	        }
	        
	        buffer.append(cmd.toString());
	    }
	    
	    // Execute script
	    try {
	        List<String> command1 = new ArrayList<>();
	        command1.add("/bin/bash");
	        command1.add(gromacsrmsdScriptPath);
	        
	        ProcessBuilder processBuilder = new ProcessBuilder(command1);
	        File outputFile = new File(user.getOutputDirPath() + "/gromacs/rmsd/scriptOutput" + (analysisWindowNumber + 1) + ".txt");
	        processBuilder.redirectOutput(outputFile);
	        processBuilder.redirectError(outputFile);
	        
	        Process process = processBuilder.start();
	        int exitCode = process.waitFor();
	        
	        System.out.println("RMSD Command exited with code: " + exitCode);
	        
	        if (exitCode != 0) {
	            System.err.println("RMSD script failed. Check output file: " + outputFile.getAbsolutePath());
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    
	    // Read results
	    String xvgFilePath = outputDirPath + "/gromacs/rmsd/" + rmsdGromacsUserInput.getOutputfileName() + ".xvg";
	    
	    return ixvgGraphReader.xvgReader(xvgFilePath);
	}
}
