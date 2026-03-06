package com.app.service;

import com.app.dto.TrajectoryFrameInput;
import org.springframework.core.io.Resource;

import java.io.IOException;

public interface IStructureFromFrameService {

    /**
     * Extract a PDB structure from a specific trajectory frame using GROMACS
     * trjconv.
     * 
     * @return the generated output filename (without full path)
     */
    String extractPdbFromFrame(TrajectoryFrameInput input) throws IOException;

    /**
     * Load the generated PDB file as a downloadable Resource.
     */
    Resource loadPdbFile(String outputFileName, String userName) throws IOException;

    /**
     * Scheduled cleanup: delete PDB files older than 6 hours.
     */
    void cleanupOldPdbFiles();
}
