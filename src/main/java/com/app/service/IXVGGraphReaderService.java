package com.app.service;

import java.util.List;

import io.jsonwebtoken.io.IOException;

public interface IXVGGraphReaderService {

	List<Double[]> xvgReader(String path) throws IOException;

}
