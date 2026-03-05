package com.app.service;

import com.app.pojos.RmsdGromacsUserInput;

import java.io.IOException;
import java.util.List;

public interface IFetchIndexFileDataService {
	public List<Double[]> fetchIndexDataGromacs(String indexFileName,String username,int analysisWindowNumber) throws IOException;

}
