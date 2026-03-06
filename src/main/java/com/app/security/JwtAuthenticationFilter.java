package com.app.security;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestHeader = request.getHeader("Authorization");
        String username = null;
        String token = null;

        if (requestHeader != null && requestHeader.startsWith("Bearer")) {
            token = requestHeader.substring(7);
            try {
                username = this.jwtHelper.getUsernameFromToken(token);
            } catch (ExpiredJwtException e) {
                logger.info("JWT token is expired!!");
                sendErrorResponse(response, 401, "TOKEN_EXPIRED", "Your session has expired. Please login again.");
                return;
            } catch (MalformedJwtException e) {
                logger.info("Invalid JWT token");
                sendErrorResponse(response, 401, "INVALID_TOKEN", "Invalid token provided");
                return;
            } catch (IllegalArgumentException e) {
                logger.info("JWT claims string is empty");
                sendErrorResponse(response, 401, "INVALID_TOKEN", "Invalid token format");
                return;
            } catch (Exception e) {
                logger.error("Token processing error", e);
                sendErrorResponse(response, 401, "AUTH_ERROR", "Authentication failed");
                return;
            }
        } else {
            logger.info("Missing or invalid Authorization header");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                Boolean validateToken = this.jwtHelper.validateToken(token, userDetails);

                if (validateToken) {
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    logger.info("Token validation failed");
                    sendErrorResponse(response, 401, "TOKEN_INVALID", "Token validation failed");
                    return;
                }
            } catch (Exception e) {
                logger.error("Authentication processing error", e);
                sendErrorResponse(response, 401, "AUTH_ERROR", "Authentication failed");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String error, String message) 
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        
        String jsonResponse = new ObjectMapper().writeValueAsString(
            new ErrorResponse(error, message)
        );
        response.getWriter().write(jsonResponse);
    }

    // Inner class for error response
    private static class ErrorResponse {
        public String error;
        public String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}