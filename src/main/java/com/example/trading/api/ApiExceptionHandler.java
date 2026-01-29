package com.example.trading.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.trading.api")
public class ApiExceptionHandler {

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<?> illegalState(IllegalStateException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
        "error", e.getMessage()
    ));
  }
}
