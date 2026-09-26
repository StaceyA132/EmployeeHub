package com.employeehub.exception; 
import org.springframework.http.HttpStatus; 
import org.springframework.http.ResponseEntity; 
import org.springframework.web.bind.annotation.ExceptionHandler; 
import org.springframework.web.bind.annotation.RestControllerAdvice; 
import java.time.Instant; import java.util.LinkedHashMap; 
import java.util.Map; 
@RestControllerAdvice 

public class GlobalExceptionHandler { 
    @ExceptionHandler(EmployeeNotFoundException.class) 
    public ResponseEntity<Map<String, Object>> handleNotFound(EmployeeNotFoundException ex) 
    { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(HttpStatus.NOT_FOUND, ex.getMessage())); } 
    
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) 
    { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(HttpStatus.BAD_REQUEST, ex.getMessage())); } private Map<String, Object> body(HttpStatus status, String message) 
    { Map<String, Object> body = new LinkedHashMap<>(); body.put("timestamp", Instant.now().toString()); body.put("status", status.value()); body.put("error", status.getReasonPhrase()); body.put("message", message); return body; } 

@ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
public ResponseEntity<Map<String, Object>> handleValidation(jakarta.validation.ConstraintViolationException ex) {
    String message = ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + " " + v.getMessage())
            .collect(java.util.stream.Collectors.joining(", "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(HttpStatus.BAD_REQUEST, message));
}


}