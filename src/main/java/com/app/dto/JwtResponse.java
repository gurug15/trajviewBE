package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponse {

	private String jwtToken;
	private String username;

}

//public void caler2() {
//	JwtResponse j =caller();
//}
//
//public jwtResponse caller() {
//	JwtResponse j = new JwtResponse();
//	abc(j);
//	return j;
//}
//
//public void abc(jwtResponse j) {
//	
//	j.setUsername("fds");
//}