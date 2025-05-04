package com.gunes.cravings.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

// Bu exception fırlatıldığında 409 Conflict dönecek (örn: zaten var olan kaynak)
// @ResponseStatus(HttpStatus.CONFLICT)
@ResponseStatus(HttpStatus.CONFLICT) // Bu exception fırlatıldığında 409 Conflict dönecek
public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}