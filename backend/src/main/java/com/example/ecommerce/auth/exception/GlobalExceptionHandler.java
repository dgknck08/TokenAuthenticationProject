package com.example.ecommerce.auth.exception;

import com.example.ecommerce.common.api.ApiErrorResponse;
import com.example.ecommerce.order.exception.OrderAccessDeniedException;
import com.example.ecommerce.order.exception.OrderNotFoundException;
import com.example.ecommerce.order.exception.ReturnRequestNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;

import io.swagger.v3.oas.annotations.Hidden;

import java.util.stream.Collectors;
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(code, message, request.getRequestURI()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserExists(UserAlreadyExistsException ex, HttpServletRequest request) {
        logger.warn("UserAlreadyExistsException: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "Kullanıcı zaten mevcut.", request);
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenRefreshException(TokenRefreshException ex, HttpServletRequest request) {
        logger.warn("TokenRefreshException: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "Token yenileme başarısız.", request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        logger.warn("InvalidCredentialsException: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Geçersiz kullanıcı bilgileri.", request);
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailNotVerified(EmailNotVerifiedException ex, HttpServletRequest request) {
        logger.warn("EmailNotVerifiedException: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "Email adresinizi doğrulamanız gerekiyor.", request);
    }

    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailVerificationException(EmailVerificationException ex, HttpServletRequest request) {
        logger.warn("EmailVerificationException: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_FAILED", ex.getMessage(), request);
    }

    @ExceptionHandler(PasswordResetException.class)
    public ResponseEntity<ApiErrorResponse> handlePasswordResetException(PasswordResetException ex, HttpServletRequest request) {
        logger.warn("PasswordResetException: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_FAILED", ex.getMessage(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        logger.warn("UserNotFoundException: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Kullanıcı bulunamadı.", request);
    }

    @ExceptionHandler(JwtValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtValidationException(JwtValidationException ex, HttpServletRequest request) {
        logger.warn("JwtValidationException: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "JWT_INVALID", "Geçersiz veya süresi dolmuş token.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, VALIDATION_ERROR_CODE, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, VALIDATION_ERROR_CODE, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        logger.warn("DataIntegrityViolationException: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "DATA_INTEGRITY_ERROR", "Veritabanı bütünlüğü ihlali.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        logger.warn("IllegalArgumentException: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, VALIDATION_ERROR_CODE, ex.getMessage(), request);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFound(OrderNotFoundException ex, HttpServletRequest request) {
        logger.warn("OrderNotFoundException: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(OrderAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderAccessDenied(OrderAccessDeniedException ex, HttpServletRequest request) {
        logger.warn("OrderAccessDeniedException: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), request);
    }

    @ExceptionHandler(ReturnRequestNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleReturnRequestNotFound(ReturnRequestNotFoundException ex, HttpServletRequest request) {
        logger.warn("ReturnRequestNotFoundException: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "RETURN_REQUEST_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(Exception ex, HttpServletRequest request) {
        logger.warn("AccessDeniedException: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Bu işlem için yetkiniz yok.", request);
    }

    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected exception", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Sunucuda beklenmedik bir hata oluştu.", request);
    }
}
