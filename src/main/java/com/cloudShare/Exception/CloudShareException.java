package com.cloudShare.Exception;

import lombok.Data;

@Data
public class CloudShareException extends RuntimeException{

    private ErrorCode errorCode;

    public CloudShareException(ErrorCode errorCode){
        super(errorCode.getErrorMessage());
        this.errorCode = errorCode;

    }

    public CloudShareException(ErrorCode errorCode, Object... args) {
        super(errorCode.formatErrorMessage(args));
        this.errorCode = errorCode;
    }

}
