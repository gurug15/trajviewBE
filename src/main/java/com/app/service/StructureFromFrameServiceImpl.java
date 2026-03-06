package com.app.service;

import com.app.dto.TrajectoryFrameInput;
import com.app.pojos.User;
import com.app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class StructureFromFrameServiceImpl implements IStructureFromFrameService {

    @Value("${variables.gmxpath}")
    private String gmxPath;

    @Autowired
    private UserRepository userRepository;

    private static final String PDB_OUTPUT_SUBDIR = "/gromacs/pdb/";
    private static final long CLEANUP_MAX_AGE_HOURS = 6;

    @Override
    public String extractPdbFromFrame(TrajectoryFrameInput input) throws IOException {
        User user = userRepository.findByEmail(input.getUserName())
                .orElseThrow(() -> new RuntimeException("User not found: " + input.getUserName()));

        Path inputDirPath = Paths.get(user.getInputfilePath()).toAbsolutePath();
        Path outputDirPath = Paths.get(user.getOutputDirPath()).toAbsolutePath();
        Path pdbOutputDir = outputDirPath.resolve("gromacs/pdb");

        // Create pdb output directory if it doesn't exist
        if (!Files.exists(pdbOutputDir)) {
            Files.createDirectories(pdbOutputDir);
            log.info("Created PDB output directory: {}", pdbOutputDir);
        }

        // Generate unique output filename to prevent collisions
        String outputFileName = "frame_" + input.getFrameNumber() + "_" + UUID.randomUUID().toString().substring(0, 8)
                + ".pdb";
        Path outputFilePath = pdbOutputDir.resolve(outputFileName);

        // Write the GROMACS trjconv shell script
        String scriptPath = pdbOutputDir + "/extractPdb_" + UUID.randomUUID().toString().substring(0, 8) + ".sh";
        try (FileWriter writer = new FileWriter(scriptPath);
                BufferedWriter buffer = new BufferedWriter(writer)) {

            // Use gmx trjconv to extract a specific frame as PDB
            // -dump specifies the time in ps to extract
            buffer.append("echo 0 | " + gmxPath + " trjconv"
                    + " -f " + inputDirPath + "/" + input.getTrajectoryFileName()
                    + " -s " + inputDirPath + "/" + input.getTopologyFileName()
                    + " -o " + outputFilePath
                    + " -dump " + input.getFrameNumber());
        }

        // Execute the shell script
        try {
            List<String> command = new ArrayList<>();
            command.add("/bin/bash");
            command.add(scriptPath);
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            File scriptOutputFile = new File(pdbOutputDir + "/extractPdb_output.txt");
            processBuilder.redirectOutput(scriptOutputFile);
            processBuilder.redirectError(scriptOutputFile);

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            log.info("PDB extraction script exited with code: {}", exitCode);

            if (exitCode != 0) {
                log.error("PDB extraction failed with exit code: {}", exitCode);
                throw new IOException("GROMACS trjconv failed with exit code: " + exitCode);
            }

            // Clean up the script file after execution
            Files.deleteIfExists(Paths.get(scriptPath));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("PDB extraction was interrupted", e);
        }

        // Verify the output file was created
        if (!Files.exists(outputFilePath)) {
            throw new IOException("PDB output file was not created: " + outputFileName);
        }

        log.info("PDB extracted successfully: {}", outputFileName);
        return outputFileName;
    }

    @Override
    public Resource loadPdbFile(String outputFileName, String userName) throws IOException {
        // Validate filename to prevent path traversal
        if (outputFileName == null || outputFileName.contains("..") || outputFileName.contains("/")
                || outputFileName.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename");
        }

        User user = userRepository.findByEmail(userName)
                .orElseThrow(() -> new RuntimeException("User not found: " + userName));

        Path outputDirPath = Paths.get(user.getOutputDirPath()).toAbsolutePath();
        Path pdbFilePath = outputDirPath.resolve("gromacs/pdb").resolve(outputFileName);

        if (!Files.exists(pdbFilePath)) {
            throw new FileNotFoundException("PDB file not found: " + outputFileName);
        }

        return new FileSystemResource(pdbFilePath);
    }

    /**
     * Scheduled task that runs every hour to clean up PDB files older than 6 hours.
     * This prevents generated PDB files from piling up on disk.
     */
    @Override
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms)
    public void cleanupOldPdbFiles() {
        log.info("Running scheduled PDB file cleanup...");
        Path analysisRoot = Paths.get("analysis").toAbsolutePath();

        if (!Files.exists(analysisRoot)) {
            return;
        }

        try (DirectoryStream<Path> userDirs = Files.newDirectoryStream(analysisRoot)) {
            for (Path userDir : userDirs) {
                Path pdbDir = userDir.resolve("gromacs/pdb");
                if (Files.exists(pdbDir) && Files.isDirectory(pdbDir)) {
                    cleanupDirectory(pdbDir);
                }
            }
        } catch (IOException e) {
            log.error("Error during PDB cleanup: {}", e.getMessage());
        }
    }

    private void cleanupDirectory(Path directory) {
        Instant cutoffTime = Instant.now().minus(CLEANUP_MAX_AGE_HOURS, ChronoUnit.HOURS);
        int deletedCount = 0;

        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.pdb")) {
            for (Path file : files) {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                    if (attrs.creationTime().toInstant().isBefore(cutoffTime)) {
                        Files.delete(file);
                        deletedCount++;
                        log.debug("Deleted old PDB file: {}", file.getFileName());
                    }
                } catch (IOException e) {
                    log.warn("Failed to delete PDB file {}: {}", file.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Error reading PDB directory {}: {}", directory, e.getMessage());
        }

        if (deletedCount > 0) {
            log.info("Cleaned up {} old PDB file(s) from {}", deletedCount, directory);
        }
    }
}
