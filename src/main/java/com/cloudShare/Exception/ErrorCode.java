package com.cloudShare.Exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    NOT_ENOUGH_CREDENDENTIALS(HttpStatus.BAD_REQUEST, "Bad Request", "Not  having Enough Credits to Upload"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Something went Wrong"),
    FILE_NOT_FOUND(HttpStatus.BAD_REQUEST, "Bad Request", "File not Found"),
    INVALID_USER(HttpStatus.BAD_REQUEST, "Bad Request", "File not belong this user"),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST,"Error creating Order","Payment Failed : %s"),
    PAYMENT_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST,"Error creating Order","Payment Verification Failed"),
    PLAN_ALREADY_EXISTS(HttpStatus.BAD_REQUEST,"You Have Already That Plan or Higher end plan","You Have Already That Plan or Higher end plan");


    private final HttpStatus httpStatus;
    private final String statusMessage;
    private final String errorMessage;

    ErrorCode(HttpStatus httpStatus, String statusMessage, String errorMessage) {
        this.httpStatus = httpStatus;
        this.statusMessage = statusMessage;
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String formatErrorMessage(Object... args) {
        return String.format(errorMessage, args);
    }
}
