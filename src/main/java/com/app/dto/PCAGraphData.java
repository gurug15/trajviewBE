package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PCAGraphData {
    
    // 3D FEL Surface data
    private List<Double> xAxis;         // PC1 coordinates
    private List<Double> yAxis;         // PC2 coordinates
    private Double[][] zAxis;           // Free Energy grid (2D array)
    
    // Raw PCA projection data
    private List<PCAPoint> pcaPoints;   // Individual PC1/PC2 points
    
    // Metadata
    private String outputFolder;
    private String status;              // SUCCESS, FAILED
    private String errorMessage;
    private Long executionTimeMs;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PCAPoint {
        private Integer frameNumber;
        private Double pc1;
        private Double pc2;
    }
}