package com.app.exc_handler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.app.cust_excs.ResourceNotFoundException;
import com.app.dto.ErrorResponse;

import jakarta.validation.ConstraintViolationException;

@ControllerAdvice // stereotype annotation . Class level annotation .
//Supplies common advice to all controllers  n rest controllers.
public class GlobalExceptionHandler
{
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<?> handleResourceNotFoundException(ResourceNotFoundException exc, WebRequest request) {
		System.out.println("in handle res not found");
		ErrorResponse errResp = new ErrorResponse(exc.getMessage(), request.getDescription(false));
		return new ResponseEntity<>(errResp, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<?> handleConstraintViolationExc(ConstraintViolationException e, WebRequest request) {
		System.out.println("in exc 2!!!");
		ErrorResponse errResp = new ErrorResponse(e.getMessage(), request.getDescription(false));
		return new ResponseEntity<>(errResp, HttpStatus.BAD_REQUEST);
	}

	// This Exception is thrown when a method argument fails validation typically
	// due to @Valid style validation
	// or request body content is required.
	//@Override

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		System.out.println("exc 3");
		
		/*
		 * List<String> validationErrs = ex.getBindingResult().getFieldErrors().stream()
		 * .map(fieldErr -> fieldErr.getDefaultMessage()).collect(Collectors.toList());
		 */	
		List<String> validationErrs=new ArrayList<>();
		//BindingResult :i/f : represents result of binding between request data n pojo properties
		for(FieldError err : ex.getBindingResult().getFieldErrors())
			validationErrs.add(err.getDefaultMessage());//adds default error messages from binding results
		
		ErrorResponse errResp=new ErrorResponse("Validation Failed", validationErrs.toString());
		return new ResponseEntity<Object>(errResp, HttpStatus.BAD_REQUEST);

	}

	// handle ANY exception
	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleAnyException(Exception exc, WebRequest request) {
		System.out.println("in handle exc");
		ErrorResponse errResp = new ErrorResponse(exc.getMessage(), request.getDescription(false));
		return new ResponseEntity<>(errResp, HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
