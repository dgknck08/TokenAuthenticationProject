package com.example.ecommerce.auth.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
	    @NotBlank(message = "Username cannot be empty")
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        @Pattern(
            regexp = "^(?=.{3,30}$)[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?$",
            message = "Username can contain letters, numbers, dot, underscore, hyphen and cannot start/end with special characters"
        )
	    String username,
	    @NotBlank(message = "Email cannot be empty")
        @Email(message = "Email format is invalid")
        @Size(max = 100, message = "Email cannot exceed 100 characters")
        @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$",
            message = "Email must include a valid domain (e.g. user@example.com)"
        )
	    String email,
	    @NotBlank(message = "Password cannot be empty")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,128}$",
            message = "Password must contain uppercase, lowercase, number and special character"
        )
	    String password,
	    @NotBlank(message = "First name cannot be empty")
        @Size(max = 50, message = "First name cannot exceed 50 characters")
        @Pattern(
            regexp = "^[\\p{L}][\\p{L}' -]{1,49}$",
            message = "First name must be 2-50 characters and contain only letters, spaces, apostrophe or hyphen"
        )
	    String firstName,
	    @NotBlank(message = "Last name cannot be empty")
        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        @Pattern(
            regexp = "^[\\p{L}][\\p{L}' -]{1,49}$",
            message = "Last name must be 2-50 characters and contain only letters, spaces, apostrophe or hyphen"
        )
	    String lastName
	) {}
