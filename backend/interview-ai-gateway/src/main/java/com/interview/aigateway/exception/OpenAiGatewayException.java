package com.interview.aigateway.exception;

public class OpenAiGatewayException extends RuntimeException {
    public OpenAiGatewayException(String message) {
        super(message);
    }

    public OpenAiGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
