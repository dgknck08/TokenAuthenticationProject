package com.example.ecommerce.auth.exception;

public class JwtValidationException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public JwtValidationException(String message) {
        super(message);
    }
    
    public JwtValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}