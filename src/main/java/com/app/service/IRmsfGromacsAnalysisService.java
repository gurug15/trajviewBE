package com.app.service;

import java.io.IOException;
import java.util.List;

import com.app.pojos.RmsfGromacsUserInput;

public interface IRmsfGromacsAnalysisService {	
	public List<Double[]> writeRmsfAnalysisScript(RmsfGromacsUserInput rmsfGromacsUserInput, String username,int analysisWindowNumber) throws IOException;

}
