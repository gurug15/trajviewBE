package com.app.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.io.IOException;

@Service
public class XvgGraphReaderServiceImpl implements IXVGGraphReaderService {

	@Override
	public List<Double[]> xvgReader(String xvgFilePath) throws IOException {
		List<Double[]> data = new ArrayList<>();

		System.out.println("Xxg File reading started..");
		System.out.println("xvgFilePath " + xvgFilePath);
//		double[][] dataArray = new double[][2];

		try (BufferedReader br = new BufferedReader(new FileReader(xvgFilePath))) {

			while (br.ready()) {

				String line = br.readLine();
				if (!line.startsWith("#") && !line.startsWith("@")) {
//					System.out.println(line); // Or process the line as needed

					String[] values = line.trim().split("\\s+");
					data.add(new Double[] { Double.parseDouble(values[0]), Double.parseDouble(values[1]) });

				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Xvg File reading Completed..");
		System.out.println("graphdata::"+data);

		return data;

	}

}
