package com.app;

import javax.annotation.Resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.app.service.IFilesStorageServiceImpl;

@SpringBootApplication
@EnableScheduling
public class TrajviewApplication {// implements CommandLineRunner {
	@Resource
	IFilesStorageServiceImpl storageService;

	public static void main(String[] args) {

		SpringApplication.run(TrajviewApplication.class, args);
	}
	// @Override
	// public void run(String... arg) throws Exception {
	// storageService.deleteAll();
	// storageService.init();
	// }

}
