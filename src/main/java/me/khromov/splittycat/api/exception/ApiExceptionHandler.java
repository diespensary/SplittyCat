package me.khromov.splittycat.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import me.khromov.splittycat.api.dto.ApiErrorResponse;
import me.khromov.splittycat.common.exception.ApplicationException;
import me.khromov.splittycat.common.exception.ConflictException;
import me.khromov.splittycat.common.exception.ForbiddenException;
import me.khromov.splittycat.common.exception.NotFoundException;
import me.khromov.splittycat.common.exception.UnauthorizedException;
import me.khromov.splittycat.common.exception.UnprocessableEntityException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationException(
            ApplicationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(resolveStatus(exception), exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String message = exception.getReason();
        if (message == null || message.isBlank()) {
            message = status.getReasonPhrase();
        }

        return buildResponse(status, message, request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getAllErrors().stream()
                .map(error -> error instanceof FieldError fieldError
                        ? fieldError.getDefaultMessage()
                        : error.getDefaultMessage())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("Некорректный запрос");

        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("Некорректный запрос");

        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    private HttpStatus resolveStatus(ApplicationException exception) {
        if (exception instanceof UnauthorizedException) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (exception instanceof ForbiddenException) {
            return HttpStatus.FORBIDDEN;
        }
        if (exception instanceof NotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (exception instanceof ConflictException) {
            return HttpStatus.CONFLICT;
        }
        if (exception instanceof UnprocessableEntityException) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message, String path) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );

        return ResponseEntity.status(status).body(body);
    }
}
