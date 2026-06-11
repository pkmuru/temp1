package com.ubs.wma.aat.rampuppack.exception;

import java.net.URI;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

/**
 * Translates application exceptions into RFC 9457 {@link ProblemDetail} responses
 * ({@code application/problem+json}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource not found");
        problem.setType(URI.create("https://docs.ubs.com/wma/aat/errors/not-found"));
        return problem;
    }

    @ExceptionHandler(MissingInsightDocumentException.class)
    public ProblemDetail handleMissingDocuments(MissingInsightDocumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Missing insight documents");
        problem.setType(URI.create("https://docs.ubs.com/wma/aat/errors/missing-insight-documents"));
        problem.setProperty("missingAceIds", ex.missingAceIds());
        return problem;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        problem.setType(URI.create("https://docs.ubs.com/wma/aat/errors/conflict"));
        return problem;
    }

    /** Unique-key or foreign-key violation (e.g. duplicate template code, deleting a referenced template). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.debug("Data integrity violation", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The request conflicts with existing data (duplicate key or referenced rows).");
        problem.setTitle("Data conflict");
        problem.setType(URI.create("https://docs.ubs.com/wma/aat/errors/data-conflict"));
        return problem;
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ProblemDetail handleValidation(WebExchangeBindException ex) {
        String detail = ex.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation failed");
        problem.setType(URI.create("https://docs.ubs.com/wma/aat/errors/validation"));
        return problem;
    }

    /**
     * Malformed body or an unconvertible value (e.g. an unknown {@code status} enum). Declared
     * after {@link WebExchangeBindException} (a subtype), which keeps its more specific handler.
     */
    @ExceptionHandler(ServerWebInputException.class)
    public ProblemDetail handleBadInput(ServerWebInputException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request body is missing, malformed, or contains an invalid value.");
        problem.setTitle("Bad request");
        problem.setType(URI.create("https://docs.ubs.com/wma/aat/errors/bad-request"));
        return problem;
    }

    /** Last-resort handler: logs the full error and returns an opaque 500 ProblemDetail. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.");
        problem.setTitle("Internal server error");
        problem.setType(URI.create("https://docs.ubs.com/wma/aat/errors/internal"));
        return problem;
    }
}
