package pl.mm.straightstreetgo.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pl.mm.straightstreetgo.api.dto.ValidationError;
import pl.mm.straightstreetgo.domain.CarUnavailableException;
import pl.mm.straightstreetgo.domain.ReservationNotFoundException;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(RuntimeException e, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(CarUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleConflict(RuntimeException e, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<ProblemDetail> handleMethodValidation(HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Request validation failed", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {
        List<ValidationError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(error.getField(), message(error)))
                .toList();

        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, "Request validation failed", request);
        problem.setProperty("errors", errors);
        return response(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedBody(HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Request body is invalid", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPathParameter(HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Request path parameter is invalid", request);
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String detail,
            HttpServletRequest request) {
        return response(status, createProblem(status, detail, request));
    }

    private static ProblemDetail createProblem(
            HttpStatus status,
            String detail,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    private static ResponseEntity<ProblemDetail> response(HttpStatus status, ProblemDetail problem) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static String message(FieldError error) {
        return Objects.toString(error.getDefaultMessage(), "Invalid value");
    }
}
