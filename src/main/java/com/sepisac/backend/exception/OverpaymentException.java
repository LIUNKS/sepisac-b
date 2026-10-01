package com.sepisac.backend.exception;

public class OverpaymentException extends RuntimeException {

    public OverpaymentException(String message) {
        super(message);
    }
}
