package com.app.service;

import com.app.pojos.GyrateGromacsUserInput;
import com.app.pojos.User;
import com.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class GyrateGromacsAnalysisServiceImpl implements IGyrateGromacsAnalysisService {

	@Value("${variables.gmxpath}")
	String gmxPath;

	@Autowired
	UserRepository userRepository;

	@Autowired
	IXVGGraphReaderService ixvgGraphReader;
	@Autowired
	IFileReaderService iFileReader;

	@Override
	public List<Double[]> writeGyrateAnalysisScript(GyrateGromacsUserInput gyrateGromacsUserInput, String username,
													int analysisWindowNumber) throws IOException {
		User user = userRepository.findByEmail(username).orElseThrow(() -> new RuntimeException("User not Found"));

		String gromacsgyrateScriptPath = user.getOutputDirPath() + "/gromacs/gyrate/gyrateGromacsScript"
				+ (analysisWindowNumber + 1) + ".sh";
		Path inputDirPath = Paths.get(user.getInputfilePath()).toAbsolutePath();

		//inputDirPath= home/nikhil/Java_Workspace/trajviewBackend/analysis/nikhilramane@gmail.com/
		Path outputDirPath = Paths.get(user.getOutputDirPath()).toAbsolutePath();
		try (FileWriter writer1 = new FileWriter(gromacsgyrateScriptPath);
				BufferedWriter buffer = new BufferedWriter(writer1)) {


			buffer.append(gmxPath + " gyrate -f " + inputDirPath + "/" + gyrateGromacsUserInput.getTrajectoryFileName()
					+ " -s " + inputDirPath + "/" + gyrateGromacsUserInput.getTopologyFileName() + " -o " + outputDirPath
					+ "/gromacs/gyrate/" + gyrateGromacsUserInput.getOutputfileName() + ".xvg" + " -n " + inputDirPath + "/"
					+ gyrateGromacsUserInput.getIndexFileName() + " -b " + gyrateGromacsUserInput.getFirstFrameno() + " -e "
					+ gyrateGromacsUserInput.getLastFrameNo() + " -acf " + outputDirPath
					+ "/gromacs/gyrate/" + gyrateGromacsUserInput.getAcfFileName()  + ".xvg"
					+ " << EOF \n" + gyrateGromacsUserInput.getGrouplsfit() + "\n" + "EOF");
		}

		try {
			List<String> command1 = new ArrayList<String>();
			command1.add("/bin/bash");
			command1.add(gromacsgyrateScriptPath);
			ProcessBuilder processBuilder = new ProcessBuilder(command1);

			processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
			File outputFile = new File(
					user.getOutputDirPath() + "/gromacs/gyrate/scriptOutput" + (analysisWindowNumber + 1) + ".txt");

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
				.xvgReader(outputDirPath + "/gromacs/gyrate/" + gyrateGromacsUserInput.getOutputfileName() + ".xvg");

	}
}
