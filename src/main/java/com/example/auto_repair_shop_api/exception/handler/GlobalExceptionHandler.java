package com.example.auto_repair_shop_api.exception.handler;

import com.example.auto_repair_shop_api.dto.error.ErrorResponse;
import com.example.auto_repair_shop_api.exception.DuplicateResourceException;
import com.example.auto_repair_shop_api.exception.InvalidSortParameterException;
import com.example.auto_repair_shop_api.exception.InvalidStateTransitionException;
import com.example.auto_repair_shop_api.exception.OwnershipViolationException;
import com.example.auto_repair_shop_api.exception.ResourceInUseException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ResourceInUseException.class)
    public ResponseEntity<ErrorResponse> handleResourceInUse(ResourceInUseException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(OwnershipViolationException.class)
    public ResponseEntity<ErrorResponse> handleOwnership(OwnershipViolationException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidStateTransitionException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Unknown sort field or direction on a list endpoint.
    @ExceptionHandler(InvalidSortParameterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSort(InvalidSortParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({UsernameNotFoundException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationFailure(Exception ex) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    // Any other Spring Security authentication problem. Without this handler the catch-all
    // below would turn a 401 into a 500.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication is required");
    }

    // Thrown by Spring Security (e.g. @PreAuthorize) when the user is logged in but lacks permission.
    // AuthorizationDeniedException extends AccessDeniedException, so it is covered too.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "You don't have permission to access this resource");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    // Validation of @PathVariable / @RequestParam when the controller class is annotated with @Validated.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fieldErrors.put(lastNodeName(violation.getPropertyPath()), violation.getMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    // Spring MVC's built-in validation of @PathVariable / @RequestParam (without @Validated on the class).
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String name = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                fieldErrors.put(name, error.getDefaultMessage());
            }
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    // Missing body, invalid JSON, or a value of the wrong type (e.g. an unknown enum constant).
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    // A path/query parameter could not be converted, e.g. /api/vehicles/abc for a Long id.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'");
    }

    // A database constraint (UNIQUE, FOREIGN KEY, CHECK, NOT NULL) rejected the change.
    // The SQL message is only logged, never returned, because it leaks table and column details.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "The data conflicts with existing data");
    }

    // Last resort for anything not handled above.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Spring MVC's own exceptions (405 method not allowed, 404 no resource, 415 unsupported media type,
        // 400 missing parameter, ...) carry their correct status. Keep it instead of turning them into 500.
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            HttpStatus resolved = HttpStatus.resolve(status.value());
            String message = resolved != null ? resolved.getReasonPhrase() : "Request failed";
            return build(status, message, Map.of());
        }

        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    private String lastNodeName(Path path) {
        String name = null;
        for (Path.Node node : path) {
            name = node.getName();
        }
        return name != null ? name : path.toString();
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return build(status, message, Map.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatusCode status, String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(
                new ErrorResponse(
                        status.value(),
                        message,
                        LocalDateTime.now(),
                        fieldErrors
                )
        );
    }
}
