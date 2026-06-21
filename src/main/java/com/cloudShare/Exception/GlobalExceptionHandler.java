package com.cloudShare.Exception;

import com.cloudShare.Response.ApiResponse;
import com.cloudShare.Response.ErrorDetails;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<?> handleDuplicateEmailException(DuplicateKeyException ex) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", HttpStatus.CONFLICT);
        data.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(data);


    }

    @ExceptionHandler(CloudShareException.class)
    public ResponseEntity<?> handleDuplicateEmailException(CloudShareException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ErrorDetails error = new ErrorDetails(errorCode.getHttpStatus().value(),
                errorCode.getErrorMessage());

        return new ResponseEntity<>(ApiResponse.failure(errorCode.getStatusMessage(), error), errorCode.getHttpStatus());


    }
}
