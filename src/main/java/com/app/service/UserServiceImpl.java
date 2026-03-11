package com.app.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.app.pojos.User;
import com.app.repository.UserRepository;

@Service
public class UserServiceImpl implements IUserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public List<User> getUsers() {
		return userRepository.findAll();
	}

	public User createUser(User user) throws IOException {
//		user.setUserId(UUID.randomUUID().toString());
		user.setCreatedDate(LocalDate.now());
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		user.setInputfilePath("analysis/" + user.getEmail() + "/gromacs/inputfiles");
		user.setOutputDirPath("analysis/" + user.getEmail() + "/");
		User u = userRepository.save(user);
		if (u != null) {
			Path directory = Paths.get("analysis");

			try {
				Files.createDirectories(directory);
				Files.createDirectory(Paths.get("analysis/" + u.getEmail()));
				Files.createDirectory(Paths.get("analysis/" + u.getEmail() + "/gromacs"));
				Files.createDirectory(Paths.get("analysis/" + u.getEmail() + "/gromacs/inputfiles"));
				Files.createDirectory(Paths.get("analysis/" + u.getEmail() + "/gromacs/rmsd"));
				Files.createDirectory(Paths.get("analysis/" + u.getEmail() + "/gromacs/rmsf"));
				Files.createDirectory(Paths.get("analysis/" + u.getEmail() + "/gromacs/gyrate"));
				Files.createDirectory(Paths.get("analysis/" + u.getEmail() + "/gromacs/pca"));

				Files.createDirectory(Paths.get("analysis/" + u.getEmail() + "/amber"));
				Files.createDirectory(Paths.get("analysis/" + u.getEmail() + "/amber/inputfiles"));
				System.out.println("Directory created successfully");
			} catch (IOException e) {
				System.err.println("Failed to create directory: " + e.getMessage());
			}

		}
		return u;
	}
}
