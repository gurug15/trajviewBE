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

	@Override
	public List<Double[]> writeRmsfAnalysisScript(RmsfGromacsUserInput rmsfGromacsUserInput, String username,
			int analysisWindowNumber) throws IOException {
		User user = userRepository.findByEmail(username).orElseThrow(() -> new RuntimeException("User not Found"));

		String gromacsrmsfScriptPath = user.getOutputDirPath() + "/gromacs/rmsf/rmsfGromacsScript"
				+ (analysisWindowNumber + 1) + ".sh";
		Path inputDirPath = Paths.get(user.getInputfilePath()).toAbsolutePath();
		Path outputDirPath = Paths.get(user.getOutputDirPath()).toAbsolutePath();
		try (FileWriter writer1 = new FileWriter(gromacsrmsfScriptPath);
				BufferedWriter buffer = new BufferedWriter(writer1)) {

			String superImpose="";
			if (rmsfGromacsUserInput.isSuperimpose()) {
				superImpose = " -fit ";
			}
			String residueRMSF="";
			if (rmsfGromacsUserInput.isResidueRMSF()) {
				residueRMSF = " -res ";
			}
			buffer.append(gmxPath + " rmsf -f " + inputDirPath + "/" + rmsfGromacsUserInput.getTrajectoryFileName()
					+ " -s " + inputDirPath + "/" + rmsfGromacsUserInput.getTopologyFileName() + " -o " + outputDirPath
					+ "/gromacs/rmsf/" + rmsfGromacsUserInput.getOutputfileName() + ".xvg" + " -n " + inputDirPath + "/"
					+ rmsfGromacsUserInput.getIndexFileName() + " -b " + rmsfGromacsUserInput.getFirstFrameno() + " -e "
					+ rmsfGromacsUserInput.getLastFrameNo() + superImpose + residueRMSF + " -dir " + outputDirPath
					+ "/gromacs/rmsf/" + rmsfGromacsUserInput.getLogFileName()
					+ " << EOF \n" + rmsfGromacsUserInput.getGrouplsfit() + "\n" + "EOF");
		}

		try {
			List<String> command1 = new ArrayList<String>();
			command1.add("/bin/bash");
			command1.add(gromacsrmsfScriptPath);
			ProcessBuilder processBuilder = new ProcessBuilder(command1);

			processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
			File outputFile = new File(
					user.getOutputDirPath() + "/gromacs/rmsf/scriptOutput" + (analysisWindowNumber + 1) + ".txt");

			processBuilder.redirectOutput(outputFile);
			processBuilder.redirectError(outputFile); // To redirect error output to the same file

			// Start the process
			Process process = processBuilder.start();

			int exitCode = process.waitFor();

			System.out.println("Command exited with code: " + exitCode);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return ixvgGraphReader
				.xvgReader(outputDirPath + "/gromacs/rmsf/" + rmsfGromacsUserInput.getOutputfileName() + ".xvg");

	}
}
