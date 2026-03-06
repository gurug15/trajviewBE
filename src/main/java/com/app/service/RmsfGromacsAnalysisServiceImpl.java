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

import com.app.pojos.RmsfGromacsUserInput;
import com.app.pojos.User;
import com.app.repository.UserRepository;

@Service
public class RmsfGromacsAnalysisServiceImpl implements IRmsfGromacsAnalysisService {

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

	/**
	 * Check if an integer is valid for GROMACS (> 0)
	 */
	private boolean isValidValue(int value) {
	    return value > 0;
	}

	/**
	 * Check if a group index is valid (>= 0, since 0 is valid in GROMACS)
	 */
	private boolean isValidGroup(int groupIndex) {
	    return groupIndex >= 0;
	}
	
	@Override
	public List<Double[]> writeRmsfAnalysisScript(RmsfGromacsUserInput rmsfGromacsUserInput, String username, int analysisWindowNumber) throws IOException {
	    User user = userRepository.findByEmail(username)
	            .orElseThrow(() -> new RuntimeException("User not Found"));
	    
	    String gromacsrmsfScriptPath = user.getOutputDirPath() + "/gromacs/rmsf/rmsfGromacsScript" + (analysisWindowNumber + 1) + ".sh";
	    Path inputDirPath = Paths.get(user.getInputfilePath()).toAbsolutePath();
	    Path outputDirPath = Paths.get(user.getOutputDirPath()).toAbsolutePath();
	    
	    Files.createDirectories(Paths.get(user.getOutputDirPath() + "/gromacs/rmsf/"));
	    
	    try (FileWriter writer1 = new FileWriter(gromacsrmsfScriptPath);
	            BufferedWriter buffer = new BufferedWriter(writer1)) {
	        
	        StringBuilder cmd = new StringBuilder();
	        cmd.append(gmxPath).append(" rmsf")
	           .append(" -f ").append(inputDirPath).append("/").append(rmsfGromacsUserInput.getTrajectoryFileName())
	           .append(" -s ").append(inputDirPath).append("/").append(rmsfGromacsUserInput.getTopologyFileName());
	        
	        cmd.append(" -o ").append(outputDirPath).append("/gromacs/rmsf/")
	           .append(rmsfGromacsUserInput.getOutputfileName()).append(".xvg");
	        
	        // Optional: Index file
	        if (isNotEmpty(rmsfGromacsUserInput.getIndexFileName())) {
	            cmd.append(" -n ").append(inputDirPath).append("/").append(rmsfGromacsUserInput.getIndexFileName());
	        }
	        
	        // Optional: First frame
	        if (isValidValue(rmsfGromacsUserInput.getFirstFrameno())) {
	            cmd.append(" -b ").append(rmsfGromacsUserInput.getFirstFrameno());
	        }
	        
	        // Optional: Last frame
	        if (isValidValue(rmsfGromacsUserInput.getLastFrameNo()) && rmsfGromacsUserInput.getLastFrameNo() > 0) {
	            cmd.append(" -e ").append(rmsfGromacsUserInput.getLastFrameNo());
	        }
	        
	        // Optional: Superimpose
	        if (rmsfGromacsUserInput.isSuperimpose()) {
	            cmd.append(" -fit");
	        }
	        
	        // Optional: Residue RMSF
	        if (rmsfGromacsUserInput.isResidueRMSF()) {
	            cmd.append(" -res");
	        }
	        
	        // Optional: Log directory
	        if (isNotEmpty(rmsfGromacsUserInput.getLogFileName())) {
	            cmd.append(" -dir ").append(outputDirPath).append("/gromacs/rmsf/")
	               .append(rmsfGromacsUserInput.getLogFileName());
	        }
	        
	        // Optional: Interactive group
	        if (isValidGroup(rmsfGromacsUserInput.getGrouplsfit())) {
	            cmd.append(" << EOF\n");
	            cmd.append(rmsfGromacsUserInput.getGrouplsfit()).append("\n");
	            cmd.append("EOF");
	        }
	        
	        buffer.append(cmd.toString());
	    }
	    
	    try {
	        List<String> command1 = new ArrayList<>();
	        command1.add("/bin/bash");
	        command1.add(gromacsrmsfScriptPath);
	        
	        ProcessBuilder processBuilder = new ProcessBuilder(command1);
	        File outputFile = new File(user.getOutputDirPath() + "/gromacs/rmsf/scriptOutput" + (analysisWindowNumber + 1) + ".txt");
	        processBuilder.redirectOutput(outputFile);
	        processBuilder.redirectError(outputFile);
	        
	        Process process = processBuilder.start();
	        int exitCode = process.waitFor();
	        
	        System.out.println("RMSF Command exited with code: " + exitCode);
	        
	        if (exitCode != 0) {
	            System.err.println("RMSF script failed. Check: " + outputFile.getAbsolutePath());
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    
	    return ixvgGraphReader.xvgReader(outputDirPath + "/gromacs/rmsf/" + rmsfGromacsUserInput.getOutputfileName() + ".xvg");
	}
}
