package com.app.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.repository.UserRepository;

@Service
public class FileReaderServiceImpl implements IFileReaderService {

	@Autowired
	UserRepository userRepository;

	@Override
	public List<String> fileReader(File inputfile) throws IOException {
		List<String> lines = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(inputfile))) {

			while (br.ready()) {
				lines.add(br.readLine());

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lines;

	}
}
