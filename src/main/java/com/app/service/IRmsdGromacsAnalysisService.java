package com.app.service;

import java.io.IOException;
import java.util.List;

import com.app.pojos.RmsdGromacsUserInput;

public interface IRmsdGromacsAnalysisService {	
	public List<Double[]> writeRmsdAnalysisScript(RmsdGromacsUserInput rmsdGromacsUserInput, String username,int analysisWindowNumber) throws IOException;

}
