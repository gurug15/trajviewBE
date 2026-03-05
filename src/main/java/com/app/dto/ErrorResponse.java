package com.app.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
	private String message;
	private LocalDateTime timeStamp;
	private String errDetails;
	
	public ErrorResponse(String message,String errDetails) {
		super();
		this.message = message;
		this.errDetails=errDetails;
		this.timeStamp=LocalDateTime.now();
	}
	}
