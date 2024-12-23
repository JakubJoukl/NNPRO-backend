package com.example.nnprorocnikovyprojekt.security;

import com.example.nnprorocnikovyprojekt.advice.GlobalExceptionHandler;
import com.example.nnprorocnikovyprojekt.advice.exceptions.NotFoundException;
import com.example.nnprorocnikovyprojekt.advice.exceptions.UnauthorizedException;
import com.example.nnprorocnikovyprojekt.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.nnprorocnikovyprojekt.services.UserService;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Retrieve the Authorization header
        try {
            String authHeader = request.getHeader("Authorization");
            String token = null;
            String username = null;

            // Check if the header starts with "Bearer "
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7); // Extract token
                username = jwtService.extractUsername(token); // Extract username from token
            }

            // If the token is valid and no authentication is set in the context
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userService.loadUserByUsername(username);
                if (!userDetails.isEnabled()) {
                    throw new UnauthorizedException("User is banned");
                }

                // Validate token and set authentication
                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    throw new UnauthorizedException("Token is not valid");
                }
            }
        } catch (UnauthorizedException e) {
            globalExceptionHandler.handleUnauthorizedException(e, request);
            throw e;
        } catch (NotFoundException e) {
            globalExceptionHandler.handleNotFoundException(e, request);
            throw e;
        } catch (Exception e) {
            globalExceptionHandler.handleAllExceptions(e, request);
            throw e;
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}

