package com.example.ecommerce.auth.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
	    @NotBlank(message = "Username cannot be empty")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
	    String username,
	    @NotBlank(message = "Email cannot be empty")
        @Email(message = "Email format is invalid")
	    String email,
	    @NotBlank(message = "Password cannot be empty")
        @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
	    String password,
	    @NotBlank(message = "First name cannot be empty")
        @Size(max = 50, message = "First name cannot exceed 50 characters")
	    String firstName,
	    @NotBlank(message = "Last name cannot be empty")
        @Size(max = 50, message = "Last name cannot exceed 50 characters")
	    String lastName
	) {}
