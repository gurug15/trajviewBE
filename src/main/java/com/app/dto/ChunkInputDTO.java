package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkInputDTO {
	private String trajectoryPath;
	private String struc_path;
	private int start_frame;
	private int chunk_size;
	}
