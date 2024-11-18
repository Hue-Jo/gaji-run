package com.service.runnersmap.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class RunnersMapExceptionHandler {

  @ExceptionHandler(RunnersMapException.class)
  public ResponseEntity<Map<String, String>> handleRunnersMapException(RunnersMapException e) {
    log.error("RunnersMapException: {}", e.getMessage());
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", e.getErrorMessage() != null ? e.getErrorMessage() : e.getMessage());
    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGeneralException(Exception e) {
    log.error("General Exception: {}", e.getMessage());
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", e.getMessage());
    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationExceptions(
      MethodArgumentNotValidException e) {
    Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            FieldError::getDefaultMessage
        ));
    return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
  }

}
