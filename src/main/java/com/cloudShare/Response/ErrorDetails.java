package com.cloudShare.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorDetails {
    private int errorCode;
    private String errorMessage;
}
