package com.luluroute.ms.carrier.exception;


import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Set;
import java.util.stream.Collectors;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@Slf4j
public class FedexCoreServiceExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    /**
     * Generic exception handler
     */
    protected ResponseEntity<Object> handleUnhandledException(Exception ex) {
        log.error("An internal error occurred. An unhandled exception was thrown. ", ex);
        return new ResponseEntity<>(ex.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    @NotNull
    /**
     * Collect the validation errors
     */
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                               HttpHeaders headers,
                                                               HttpStatus status, WebRequest request) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(x -> "%s: %s".formatted(x.getField(), x.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        String message = String.format("Invalid inbound request method argument(s). [%s]", errors);
        return super.handleExceptionInternal(ex, message, headers, status, request);

    }

    @Override
    @NotNull
    /**
     * Override super to return error in response body
     */
    public ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                      HttpHeaders headers, HttpStatus status,
                                                                      WebRequest request) {
        pageNotFoundLogger.error(ex.getMessage());
        Set<HttpMethod> supportedMethods = ex.getSupportedHttpMethods();
        if (!CollectionUtils.isEmpty(supportedMethods)) {
            headers.setAllow(supportedMethods);
        }

        return this.handleExceptionInternal(ex, ex.getMessage(), headers, status, request);
    }

    @Override
    @NotNull
    /**
     * Override super to return error in response body
     */
    public ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                               HttpHeaders headers, HttpStatus status,
                                                               WebRequest request) {
        return this.handleExceptionInternal(ex, ex.getMessage(), headers, status, request);
    }


    @Override
    @NotNull
    /**
     * Override super to return error in response body
     */
    public ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                       HttpHeaders headers, HttpStatus status,
                                                                       WebRequest request) {
        return this.handleExceptionInternal(ex, ex.getMessage(), headers, status, request);
    }

    @Override
    /**
     * Override super to include a log statement on all handling
     */
    public ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
                                                          HttpStatus status, WebRequest request) {
        log.error("Global exception handler handling: ", ex);
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

}