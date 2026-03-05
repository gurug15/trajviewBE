package com.app.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.pojos.User;
import com.app.service.UserServiceImpl;

@RestController
@RequestMapping("/home")
//@CrossOrigin(origins = "*" , allowedHeaders = {"Authorization"})

public class HomeController {
	@Autowired
	UserServiceImpl userService;
	public HomeController() {
	}

	@GetMapping("/user")
	public List<User> getUser() {
//		
//		System.out.println("getting users");
//		headers.forEach((key, value) -> {
//            System.out.println(key + ": " + value);
//        });
		return this.userService.getUsers();
	}
	
	@GetMapping("/current-user")
	public String getLoggedInUser(Principal principal) {
		return principal.getName();
	}
	
	
}
