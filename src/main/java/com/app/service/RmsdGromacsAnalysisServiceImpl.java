package com.app.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
	public List<Double[]> writeRmsdAnalysisScript(RmsdGromacsUserInput rmsdGromacsUserInput,String username,int analysisWindowNumber) throws IOException {
		User user = userRepository.findByEmail(username).orElseThrow(()-> new RuntimeException("User not Found"));

		String gromacsrmsdScriptPath = user.getOutputDirPath() + "/gromacs/rmsd/rmsdGromacsScript"+(analysisWindowNumber+1)+".sh";
		Path inputDirPath=Paths.get(user.getInputfilePath()).toAbsolutePath();
		Path outputDirPath=Paths.get(user.getOutputDirPath()).toAbsolutePath();
		try (FileWriter writer1 = new FileWriter(gromacsrmsdScriptPath);
				BufferedWriter buffer = new BufferedWriter(writer1)) {

			buffer.append(gmxPath + " rms -f " + inputDirPath + "/"
					+ rmsdGromacsUserInput.getTrajectoryFileName() + " -s " + inputDirPath + "/"
					+ rmsdGromacsUserInput.getTopologyFileName() + " -n " + inputDirPath + "/"
					+ rmsdGromacsUserInput.getIndexFileName() + " -o " + outputDirPath
					+ "/gromacs/rmsd/" + rmsdGromacsUserInput.getOutputfileName() + ".xvg" + " -b "
					+ rmsdGromacsUserInput.getFirstFrameno() + " -e " + rmsdGromacsUserInput.getLastFrameNo()
					+ " << EOF \n" + rmsdGromacsUserInput.getGrouplsfit() + "\n" + rmsdGromacsUserInput.getGroupRMSD()
					+ "\n" + "EOF");
		}

		try {
			List<String> command1 = new ArrayList<String>();
			command1.add("/bin/bash");
			command1.add(gromacsrmsdScriptPath);
			ProcessBuilder processBuilder = new ProcessBuilder(command1);

			processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
			File outputFile = new File(user.getOutputDirPath() + "/gromacs/rmsd/scriptOutput"+(analysisWindowNumber+1)+".txt");

			processBuilder.redirectOutput(outputFile);
			processBuilder.redirectError(outputFile); // To redirect error output to the same file

			// Start the process
			Process process = processBuilder.start();

			int exitCode = process.waitFor();

//			 if (outputFile.createNewFile()) {
//	                System.out.println("File created successfully: " + outputFile.getName());
//	            } else {
//	                System.out.println("File already exists.");
//	            }
//			 

			System.out.println("Command exited with code: " + exitCode);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return ixvgGraphReader.xvgReader(outputDirPath+ "/gromacs/rmsd/" + rmsdGromacsUserInput.getOutputfileName() + ".xvg");

	}
}
