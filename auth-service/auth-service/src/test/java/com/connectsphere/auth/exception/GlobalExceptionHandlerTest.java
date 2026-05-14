package com.connectsphere.auth.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * GlobalExceptionHandlerTest — Unit tests for the centralized exception handler.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    @DisplayName("handleNotFound — should return 404 response")
    void handleNotFound_Returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    @DisplayName("handleConflict — should return 409 response")
    void handleConflict_Returns409() {
        ConflictException ex = new ConflictException("Email already exists");
        ResponseEntity<ApiErrorResponse> response = handler.handleConflict(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email already exists", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleBadRequest — should return 400 response")
    void handleBadRequest_Returns400() {
        BadRequestException ex = new BadRequestException("Invalid input");
        ResponseEntity<ApiErrorResponse> response = handler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid input", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleValidation — should return 400 with field errors")
    void handleValidation_Returns400WithErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "email", "must be valid");
        when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
        
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(MethodParameter.class), bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getValidationErrors());
        assertEquals("must be valid", response.getBody().getValidationErrors().get("email"));
    }

    @Test
    @DisplayName("handleUnexpected — should return 500 response")
    void handleUnexpected_Returns500() {
        Exception ex = new RuntimeException("DB down");
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Something went wrong. Please try again.", response.getBody().getMessage());
    }
}
