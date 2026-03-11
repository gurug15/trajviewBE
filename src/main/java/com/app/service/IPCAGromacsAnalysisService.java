package com.app.service;

import java.io.IOException;

import com.app.dto.PCAGraphData;
import com.app.pojos.PCAGromacsUserInput;

public interface IPCAGromacsAnalysisService {
	public PCAGraphData getPcaGraphData(PCAGromacsUserInput pcaGromacsUserInput, String username,int analysisWindowNumber) throws IOException;
}
