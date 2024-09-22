package com.ivan_degtev.telegrambotforpapablinov.exception;

public class LlmQuerySyntaxException extends RuntimeException {
    public LlmQuerySyntaxException(String message) {
        super(message);
    }

}