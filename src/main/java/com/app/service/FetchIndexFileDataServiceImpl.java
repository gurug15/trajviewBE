package com.app.service;

import com.app.pojos.RmsdGromacsUserInput;
import com.app.pojos.User;
import com.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class FetchIndexFileDataServiceImpl implements IFetchIndexFileDataService {

	@Value("${variables.gmxpath}")
	String gmxPath;
	
	@Autowired
	UserRepository userRepository;

///Not using in Code
	@Override
	public List<Double[]> fetchIndexDataGromacs(String indexFileName,String username,int analysisWindowNumber) throws IOException {
		User user = userRepository.findByEmail(username).orElseThrow(()-> new RuntimeException("User not Found"));
		String indexFileScriptPath = user.getOutputDirPath() + "/gromacs/rmsd/indexFileScript"+(analysisWindowNumber+1)+".sh";
		Path inputDirPath=Paths.get(user.getInputfilePath()).toAbsolutePath();
		try (FileWriter writer1 = new FileWriter(indexFileScriptPath);
				BufferedWriter buffer = new BufferedWriter(writer1)) {

			buffer.append(gmxPath + " make_ndx -n "
					+ inputDirPath + "/"
					+ indexFileName
					+ " << EOF"
					+ "\n" + "EOF");
		}

		try {
			List<String> command1 = new ArrayList<String>();
			command1.add("/bin/bash");
			command1.add(indexFileScriptPath);
			ProcessBuilder processBuilder = new ProcessBuilder(command1);

			processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

			//write indexfile script output in array
			Process process = processBuilder.start();

			// Read the output of the process
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder outputBuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				outputBuilder.append(line).append("\n");
			}
			reader.close();

			// Get the output as a string
			String output = outputBuilder.toString();

			int exitCode = process.waitFor();
			System.out.println("Command exited with code: " + exitCode);
			System.out.println("Output:\n" + output);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}
}
