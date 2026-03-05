package com.app.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface IFileReaderService {	
	public List<String> fileReader(File inputfile) throws IOException;
}



