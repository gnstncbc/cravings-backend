package com.gunes.cravings.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Bu exception fırlatıldığında 404 Not Found dönecek
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}



// Genel Hata DTO'su (İsteğe bağlı, daha detaylı hata mesajları için)
// @Data @NoArgsConstructor @AllArgsConstructor
// public class ErrorResponse {
//     private LocalDateTime timestamp;
//     private int status;
//     private String error;
//     private String message;
//     private String path;
// }