package com.app.service;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
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

import com.app.dto.PCAGraphData;
import com.app.dto.PCAGraphData.PCAPoint;
import com.app.pojos.PCAGromacsUserInput;
import com.app.pojos.User;
import com.app.repository.UserRepository;

@Service
public class PCAGromacsAnalysisServiceImpl implements IPCAGromacsAnalysisService {

    @Value("${variables.gmxpath}")
    private String gmxPath;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IXVGGraphReaderService xvgGraphReader;

    /**
     * Execute PCA analysis using the FEL analysis script
     */
    @Override
    public PCAGraphData getPcaGraphData(
            PCAGromacsUserInput pcaInput,
            String username,
            int analysisWindowNumber
    ) throws IOException {
        
        long startTime = System.currentTimeMillis();
        PCAGraphData result = new PCAGraphData();
        
        try {
            // Get user and validate
            User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Prepare paths
            Path inputDirPath = Paths.get(user.getInputfilePath()).toAbsolutePath();
            Path outputDirPath = Paths.get(user.getOutputDirPath() + "/gromacs/pca/" 
                + pcaInput.getOutputFolder()).toAbsolutePath();

            // Create output directory
            Files.createDirectories(outputDirPath);

            // Validate input files exist
            validateInputFiles(inputDirPath, pcaInput);

            // Write and execute the FEL analysis script
            executeFELAnalysisScript(
                inputDirPath,
                outputDirPath,
                pcaInput,
                analysisWindowNumber
            );

            // Parse results and return graph data
            result = parseAnalysisResults(outputDirPath, pcaInput);
            result.setStatus("SUCCESS");
            result.setOutputFolder(outputDirPath.toString());

        } catch (Exception e) {
            System.err.println("PCA Analysis failed: " + e.getMessage());
            e.printStackTrace();
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage());
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Validate input files exist
     */
    private void validateInputFiles(Path inputDirPath, PCAGromacsUserInput pcaInput) throws IOException {
        Path xtcFile = inputDirPath.resolve(pcaInput.getTrajectoryFile());
        Path tprFile = inputDirPath.resolve(pcaInput.getTprFile());
        Path ndxFile = inputDirPath.resolve(pcaInput.getIndexFile());

        if (!Files.exists(xtcFile)) {
            throw new IOException("Trajectory file not found: " + xtcFile);
        }
        if (!Files.exists(tprFile)) {
            throw new IOException("TPR file not found: " + tprFile);
        }
        if (!Files.exists(ndxFile)) {
            throw new IOException("Index file not found: " + ndxFile);
        }
    }

    /**
     * Execute the FEL analysis bash script
     */
    private void executeFELAnalysisScript(
            Path inputDirPath,
            Path outputDirPath,
            PCAGromacsUserInput pcaInput,
            int analysisWindowNumber
    ) throws Exception {
        
        // Create bash script
        String scriptPath = outputDirPath + "/pca_analysis_" + (analysisWindowNumber + 1) + ".sh";
        
        // Get the path to the fel_analysis_dynamic.sh script (should be in resources or external location)
        String felScriptPath = "./scripts/fel_analysis_dynamic.sh"; // Update this path
        
        // Prepare absolute paths for input files
        String xtcPath = inputDirPath.resolve(pcaInput.getTrajectoryFile()).toString();
        String tprPath = inputDirPath.resolve(pcaInput.getTprFile()).toString();
        String ndxPath = inputDirPath.resolve(pcaInput.getIndexFile()).toString();

        // Build command to call the FEL script
        StringBuilder cmd = new StringBuilder();
        cmd.append("bash ").append(felScriptPath);
        cmd.append(" --xtc ").append("\"").append(xtcPath).append("\"");
        cmd.append(" --tpr ").append("\"").append(tprPath).append("\"");
        cmd.append(" --ndx ").append("\"").append(ndxPath).append("\"");
        cmd.append(" --output ").append("\"").append(outputDirPath).append("\"");

        if (pcaInput.getStepSize() != null && pcaInput.getStepSize() > 0) {
            cmd.append(" --step ").append(pcaInput.getStepSize());
        }
        if (pcaInput.getMaxTime() != null && pcaInput.getMaxTime() > 0) {
            cmd.append(" --max-time ").append(pcaInput.getMaxTime());
        }

        // Write command to script file
        try (FileWriter writer = new FileWriter(scriptPath);
             BufferedWriter buffer = new BufferedWriter(writer)) {
            buffer.write("#!/bin/bash\n");
            buffer.write(cmd.toString());
        }

        // Execute the script
        List<String> command = new ArrayList<>();
        command.add("/bin/bash");
        command.add(scriptPath);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        File outputFile = new File(outputDirPath + "/script_output.txt");
        processBuilder.redirectOutput(outputFile);
        processBuilder.redirectError(outputFile);

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String errorMsg = readFileContent(outputFile);
            throw new RuntimeException("Script execution failed with exit code: " + exitCode + "\n" + errorMsg);
        }

        System.out.println("PCA Script executed successfully");
    }

    /**
     * Parse analysis results and create PCAGraphData
     */
    private PCAGraphData parseAnalysisResults(Path outputDirPath, PCAGromacsUserInput pcaInput) 
            throws IOException {
        
        PCAGraphData graphData = new PCAGraphData();

        // Read PCA projection data from pca_2dproj.xvg
        String projectionFile = outputDirPath + "/pca_2dproj.xvg";
        List<PCAPoint> pcaPoints = parsePCAProjection(projectionFile);
        graphData.setPcaPoints(pcaPoints);

        // Create FEL grid from PCA points
        FELGrid felGrid = createFELGrid(pcaPoints);
        graphData.setXAxis(felGrid.getXAxis());
        graphData.setYAxis(felGrid.getYAxis());
        graphData.setZAxis(felGrid.getZAxis());

        return graphData;
    }

    /**
     * Parse pca_2dproj.xvg file and extract PCA points
     */
    private List<PCAPoint> parsePCAProjection(String filePath) throws IOException {
        List<PCAPoint> points = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int frameNumber = 0;

            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();

                // Skip comments and headers
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("@")) {
                    continue;
                }

                String[] values = trimmed.split("\\s+");
                if (values.length >= 2) {
                    try {
                        Double pc1 = Double.parseDouble(values[0]);
                        Double pc2 = Double.parseDouble(values[1]);

                        PCAPoint point = new PCAPoint();
                        point.setFrameNumber(frameNumber++);
                        point.setPc1(pc1);
                        point.setPc2(pc2);

                        points.add(point);
                    } catch (NumberFormatException e) {
                        // Skip invalid lines
                    }
                }
            }
        }

        System.out.println("Parsed " + points.size() + " PCA points");
        return points;
    }

    /**
     * Create FEL grid from PCA points
     */
    private FELGrid createFELGrid(List<PCAPoint> points) {
        if (points.isEmpty()) {
            return new FELGrid();
        }

        // Find min/max values
        double pc1Min = points.stream().mapToDouble(PCAPoint::getPc1).min().orElse(0);
        double pc1Max = points.stream().mapToDouble(PCAPoint::getPc1).max().orElse(1);
        double pc2Min = points.stream().mapToDouble(PCAPoint::getPc2).min().orElse(0);
        double pc2Max = points.stream().mapToDouble(PCAPoint::getPc2).max().orElse(1);

        // Add padding
        double pc1Range = pc1Max - pc1Min;
        if (pc1Range == 0) pc1Range = 1;
        double pc2Range = pc2Max - pc2Min;
        if (pc2Range == 0) pc2Range = 1;

        double pc1MinPad = pc1Min - pc1Range * 0.1;
        double pc1MaxPad = pc1Max + pc1Range * 0.1;
        double pc2MinPad = pc2Min - pc2Range * 0.1;
        double pc2MaxPad = pc2Max + pc2Range * 0.1;

        // Create grid
        int gridSize = 150;
        int[][] grid = new int[gridSize][gridSize];

        // Bin the data
        for (PCAPoint point : points) {
            int xIdx = (int) Math.floor(
                ((point.getPc1() - pc1MinPad) / (pc1MaxPad - pc1MinPad)) * (gridSize - 1)
            );
            int yIdx = (int) Math.floor(
                ((point.getPc2() - pc2MinPad) / (pc2MaxPad - pc2MinPad)) * (gridSize - 1)
            );

            if (xIdx >= 0 && xIdx < gridSize && yIdx >= 0 && yIdx < gridSize) {
                grid[yIdx][xIdx]++;
            }
        }

        // Convert density to free energy: G = -RT ln(P)
        int maxCount = 0;
        for (int[] row : grid) {
            for (int count : row) {
                if (count > maxCount) maxCount = count;
            }
        }

        Double[][] felGrid = new Double[gridSize][gridSize];
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                double density = grid[y][x] / (double) maxCount;
                if (density > 0) {
                    felGrid[y][x] = -Math.log(density) * 5;
                } else {
                    felGrid[y][x] = 20.0;
                }
            }
        }

        // Create axis arrays
        List<Double> xAxis = new ArrayList<>();
        for (int i = 0; i < gridSize; i++) {
            xAxis.add(pc1MinPad + (i / (double) (gridSize - 1)) * (pc1MaxPad - pc1MinPad));
        }

        List<Double> yAxis = new ArrayList<>();
        for (int i = 0; i < gridSize; i++) {
            yAxis.add(pc2MinPad + (i / (double) (gridSize - 1)) * (pc2MaxPad - pc2MinPad));
        }

        FELGrid result = new FELGrid();
        result.setXAxis(xAxis);
        result.setYAxis(yAxis);
        result.setZAxis(felGrid);
        return result;
    }

    /**
     * Read file content
     */
    private String readFileContent(File file) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            content.append("Error reading file: ").append(e.getMessage());
        }
        return content.toString();
    }

    /**
     * Inner class for FEL grid data
     */
    private static class FELGrid {
        private List<Double> xAxis;
        private List<Double> yAxis;
        private Double[][] zAxis;

        public List<Double> getXAxis() {
            return xAxis;
        }

        public void setXAxis(List<Double> xAxis) {
            this.xAxis = xAxis;
        }

        public List<Double> getYAxis() {
            return yAxis;
        }

        public void setYAxis(List<Double> yAxis) {
            this.yAxis = yAxis;
        }

        public Double[][] getZAxis() {
            return zAxis;
        }

        public void setZAxis(Double[][] zAxis) {
            this.zAxis = zAxis;
        }
    }
}