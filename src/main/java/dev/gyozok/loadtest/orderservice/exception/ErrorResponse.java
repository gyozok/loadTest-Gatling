package dev.gyozok.loadtest.orderservice.exception;

import java.time.Instant;
import java.util.List;

/**
 * This return as an error body for every handled exception
 * API clients can rely on one shape regardless what went wrong
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors
) {

    public record FieldError(
            String field,
            String errorMessage
    ){}

    public static ErrorResponse of(int status, String error, String message) {
        return of(
                status,
                error,
                message,
                List.of()
        );
    }

    public static ErrorResponse of(int status, String error, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                fieldErrors
        );
    }
}
