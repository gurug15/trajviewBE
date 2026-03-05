package com.app.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO {
	private String message;
	private LocalDateTime ts;
	public ResponseDTO(String message) {
		super();
		this.message = message;
		this.ts=LocalDateTime.now();
	}
	}
