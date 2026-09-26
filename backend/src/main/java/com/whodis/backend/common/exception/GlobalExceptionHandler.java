package com.whodis.backend.common.exception;

import com.whodis.backend.person.service.DuplicatePersonException;
import com.whodis.backend.person.service.PersonNotFoundException;
import com.whodis.backend.referenceimage.service.InvalidImageException;
import com.whodis.backend.session.service.SessionExpiredException;
import com.whodis.backend.session.service.SessionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionNotFound(
            SessionNotFoundException exception
    ) {

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "SESSION_NOT_FOUND",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionExpired(
            SessionExpiredException exception
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.GONE.value(),
                "SESSION_EXPIRED",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.GONE)
                .body(response);
    }

    @ExceptionHandler(PersonNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePersonNotFound(
            PersonNotFoundException exception
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "PERSON_NOT_FOUND",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(DuplicatePersonException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicatePerson(
            DuplicatePersonException exception
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "DUPLICATE_PERSON",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationError(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                message
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidImage(
            InvalidImageException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(new ApiErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "INVALID_IMAGE",
                        exception.getMessage()
                ));
    }
}
