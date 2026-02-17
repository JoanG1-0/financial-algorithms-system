package com.financial.etl.exception;

public class DataDownloadException extends RuntimeException {

    public DataDownloadException(String message) {
        super(message);
    }

    public DataDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
