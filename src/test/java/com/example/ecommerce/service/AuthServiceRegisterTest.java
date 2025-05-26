package com.example.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;

import com.example.ecommerce.auth.dto.RegisterRequest;
import com.example.ecommerce.auth.dto.RegisterResponse;
import com.example.ecommerce.auth.exception.UserAlreadyExistsException;
import com.example.ecommerce.auth.model.RefreshToken;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.service.AuthService;
import com.example.ecommerce.auth.service.RefreshTokenService;
import com.example.ecommerce.auth.service.UserService;

public class AuthServiceRegisterTest {

		private UserService userService;
	    private JwtTokenProvider jwtTokenProvider;
	    private RefreshTokenService refreshTokenService;
	    private AuthenticationManager authenticationManager;
	    private AuthService authService;
	
	    @BeforeEach
	    void setUp() {
	        userService = mock(UserService.class);
	        jwtTokenProvider = mock(JwtTokenProvider.class);
	        refreshTokenService = mock(RefreshTokenService.class);
	        authenticationManager = mock(AuthenticationManager.class);
	
	        authService = new AuthService(userService, jwtTokenProvider, refreshTokenService, authenticationManager);
	    }
	    
	    
	    @Test
	    public void register_ShouldReturnRegisterResponse_whenCredentialsAreValid() {
	    	String username = "testuser1";
	        String password = "testpassword";
	        String firstName = "test";
	        String lastName = "user";
	        String email = "test@example.com"; 
	    	
	        RegisterRequest registerRequest = new RegisterRequest(username,
	        		password,
	        		firstName,
	        		lastName
	        		,email);
	        
	        User mockUser = new User();
	        mockUser.setId(1L);
	        mockUser.setUsername(username);
	        mockUser.setEmail(email);
	        
	        String fakeAccessToken = "access-token";
	        String fakeRefreshToken = "refresh-token";
	        // mocklama
	        when(userService.findByUsername(username)).thenReturn(Optional.empty());
	        when(userService.findByEmail(email)).thenReturn(Optional.empty());
	        when(userService.createUser(registerRequest)).thenReturn(mockUser);
	        when(jwtTokenProvider.generateTokenWithUsername(username)).thenReturn(fakeAccessToken);
	        when(refreshTokenService.createRefreshToken(mockUser.getId()))
	            .thenReturn(new RefreshToken(fakeRefreshToken));
	        
	        RegisterResponse response = authService.register(registerRequest);

	        // Assert
	        assertEquals(username, response.getUsername());
	        assertEquals(email, response.getEmail());
	        assertEquals(fakeAccessToken, response.getAccessToken());
	        assertEquals(fakeRefreshToken, response.getRefreshToken());
	        
	    }
	    @Test
	    void register_ShouldThrowException_whenUsernameAlreadyExists() {
	        // Arrange
	        RegisterRequest request = new RegisterRequest("existinguser", "pass", "Test", "User", "new@example.com");

	        when(userService.findByUsername("existinguser")).thenReturn(Optional.of(new User()));

	        // Act + Assert
	        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));

	        verify(userService).findByUsername("existinguser");
	        verify(userService, never()).createUser(any());
	    }
	    
	  
	   

	    @Test
	    void register_ShouldThrowException_WhenEmailExists() {
	        // arrange
	        RegisterRequest request = new RegisterRequest("newuser", "used@example.com", "password", "ahmet", "hamdi");
	        when(userService.findByUsername("newuser")).thenReturn(Optional.empty());
	        when(userService.findByEmail("used@example.com")).thenReturn(Optional.of(new User()));

	        // act & assert
	        UserAlreadyExistsException exception = assertThrows(
	            UserAlreadyExistsException.class,
	            () -> authService.register(request)
	        );

	        assertEquals("Email is already registered", exception.getMessage());
	        verify(userService, never()).createUser(any());
	    }

	
	

}
