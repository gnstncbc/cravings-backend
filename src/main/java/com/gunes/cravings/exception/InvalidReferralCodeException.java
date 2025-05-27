package com.gunes.cravings.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidReferralCodeException extends RuntimeException {
    public InvalidReferralCodeException(String message) {
        super(message);
    }
}