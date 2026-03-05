package com.app.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

import com.app.dto.ChunkInputDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import org.springframework.core.io.Resource;

@RestController
@RequestMapping("/api")
//@CrossOrigin(origins = "*", allowedHeaders = "*")

public class TrajectoryChunkController {
	@Autowired
	private WebClient.Builder webClientBuilder;

	private final String pythonServiceUrl = "http://localhost:5000";

/*
	@PostMapping("/initialize")
	public Mono<Map> initialize(@RequestBody Map<String, String> request,ChunkInputDTO chunkInputDTO) {
		System.out.println("In initialize method");


		HttpClient httpClient = HttpClient.create()
				.responseTimeout(Duration.ofSeconds(300)) // Increase timeout if needed
				.doOnConnected(conn ->
						conn.addHandlerLast(new ReadTimeoutHandler(300))
								.addHandlerLast(new WriteTimeoutHandler(300))
				);

		WebClient webClient = webClientBuilder
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // Set to 10MB or adjust as needed
				.build();


		Mono<Map> initializeresponse = null;
		try {

		initializeresponse= webClientBuilder.build()
				.post()
				.uri(pythonServiceUrl + "/initialize")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Map.class)
				.doOnNext(response -> System.out.println("Response: " + response))
				.doOnError(WebClientResponseException.class, e -> {
					System.err.println("WebClientResponseException: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
				});
		System.out.println("Mono Map initializeresponse::"+initializeresponse.toString());
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return initializeresponse;
	}
*/

	@PostMapping("/fetch_chunk")
	public Mono<Map> fetchChunk(@RequestBody ChunkInputDTO chunkInputDTO) {


		System.out.println("In fetch_chunk method");
		System.out.println("In fetch_chunk method Struct filename::"+chunkInputDTO.getStruc_path());


		try {
			// Find and kill process running on port 5000
			ProcessBuilder findProcess = new ProcessBuilder("bash", "-c", "sudo lsof -ti :5000 | xargs sudo kill -9");
			Process killProcess = findProcess.start();
			killProcess.waitFor(); // Wait for the command to complete
			System.out.println("Killed process on port 5000");
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			ProcessBuilder runPython = new ProcessBuilder("python3", "trajectory_service.py");
			Process pythonProcess = runPython.start();
			System.out.println("Started Python script trajectory_service.py");

			// Capture and log the output
			BufferedReader reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));
			new Thread(() -> {
				String line;
				try {
					while ((line = reader.readLine()) != null) {
						System.out.println("[trajectory_service.py]: " + line);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}).start();

			// Optionally, you can wait for a short period to ensure the Python script starts
			Thread.sleep(2000); // Wait 2 seconds
		} catch (Exception e) {
			e.printStackTrace();
		}


		HttpClient httpClient = HttpClient.create()
				.responseTimeout(Duration.ofSeconds(300)) // Increase timeout if needed
				.doOnConnected(conn ->
						conn.addHandlerLast(new ReadTimeoutHandler(300))
								.addHandlerLast(new WriteTimeoutHandler(300))
				);

		WebClient webClient = webClientBuilder
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024 * 2046)) // Set to 10MB or adjust as needed
				.build();

		Mono<Map> fetchChunkresponse = null;
		try {
			fetchChunkresponse = webClient
					.post()
					.uri(pythonServiceUrl + "/fetch_chunk")
					.bodyValue(chunkInputDTO)
					.retrieve()
					.bodyToMono(Map.class)
				.doOnNext(response -> System.out.println("Response: " + response))
				.doOnError(WebClientResponseException.class, e -> {
					System.err.println("WebClientResponseException: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
				});
//			System.out.println("Mono Map fetchChunkresponse::" + fetchChunkresponse);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return fetchChunkresponse;
	}



	@GetMapping("/structurefileContentforvisualization")
	public String getFileContent(@RequestParam("struct_path") String struct_path) {
		try {
			System.out.println("In content::"+struct_path);

			// Read the content of the file as a String
			return new String(Files.readAllBytes(Paths.get(struct_path)));
		} catch (IOException e) {
			e.printStackTrace();
			return "File not found or error reading the file.";
		}
	}
}