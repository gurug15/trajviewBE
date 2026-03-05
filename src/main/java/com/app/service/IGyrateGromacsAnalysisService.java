package com.app.service;

import com.app.pojos.GyrateGromacsUserInput;

import java.io.IOException;
import java.util.List;

public interface IGyrateGromacsAnalysisService {
	public List<Double[]> writeGyrateAnalysisScript(GyrateGromacsUserInput gyrateGromacsUserInput, String username, int analysisWindowNumber) throws IOException;

}
