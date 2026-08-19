package com.xcess.ocs.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.xcess.ocs.dto.ErrorResponseDTO;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.entity.UnitType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

        @Override
        protected ResponseEntity<Object> handleMethodArgumentNotValid(
                        @NonNull MethodArgumentNotValidException ex,
                        @NonNull HttpHeaders headers,
                        @NonNull HttpStatusCode status,
                        @NonNull WebRequest request) {
                Map<String, String> validationErrors = new HashMap<>();
                List<ObjectError> validationErrorList = ex.getBindingResult().getAllErrors();

                validationErrorList.forEach((error) -> {
                        String fieldName = ((FieldError) error).getField();
                        String validationMsg = error.getDefaultMessage();
                        validationErrors.put(fieldName, validationMsg);
                });
                return new ResponseEntity<>(validationErrors, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponseDTO> handleGlobalException(Exception exception,
                        WebRequest webRequest) {
                ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                                webRequest.getDescription(false),
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                exception.getMessage(),
                                LocalDateTime.now());
                return new ResponseEntity<>(errorResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(ResourceNotFoundException exception,
                        WebRequest webRequest) {
                ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                                webRequest.getDescription(false),
                                HttpStatus.NOT_FOUND,
                                exception.getMessage(),
                                LocalDateTime.now());
                return new ResponseEntity<>(errorResponseDTO, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(DuplicateNameException.class)
        public ResponseEntity<ErrorResponseDTO> handleDuplicateNameException(DuplicateNameException exception,
                        WebRequest webRequest) {
                ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                                webRequest.getDescription(false),
                                HttpStatus.CONFLICT,
                                exception.getMessage(),
                                LocalDateTime.now());
                return new ResponseEntity<>(errorResponseDTO, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(DuplicatePartnerException.class)
        public ResponseEntity<ErrorResponseDTO> handleDuplicatePartnerException(DuplicatePartnerException exception,
                        WebRequest webRequest) {
                ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                                webRequest.getDescription(false),
                                HttpStatus.CONFLICT,
                                exception.getMessage(),
                                LocalDateTime.now());
                return new ResponseEntity<>(errorResponseDTO, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolationException(
                        DataIntegrityViolationException exception,
                        WebRequest webRequest) {
                ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                                webRequest.getDescription(false),
                                HttpStatus.CONFLICT,
                                "Data integrity violation: Duplicate or invalid data",
                                LocalDateTime.now());
                return new ResponseEntity<>(errorResponseDTO, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(InvalidInputException.class)
        public ResponseEntity<ErrorResponseDTO> handleInvalidInputException(InvalidInputException exception,
                        WebRequest webRequest) {
                ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                                webRequest.getDescription(false),
                                HttpStatus.BAD_REQUEST,
                                exception.getMessage(),
                                LocalDateTime.now());
                return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(InvalidDateTimeFormatException.class)
        public ResponseEntity<ErrorResponseDTO> handleInvalidDateTimeFormatException(
                        InvalidDateTimeFormatException exception,
                        WebRequest webRequest) {
                ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                                webRequest.getDescription(false),
                                HttpStatus.BAD_REQUEST,
                                exception.getMessage(),
                                LocalDateTime.now());
                return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
        }

        @Override
        protected ResponseEntity<Object> handleHttpMessageNotReadable(
                        HttpMessageNotReadableException ex,
                        @NonNull HttpHeaders headers,
                        @NonNull HttpStatusCode status,
                        @NonNull WebRequest request) {

                String errorMessage = "Invalid request body";

                // Check if it's a format issue
                Throwable cause = ex.getCause();
                if (cause instanceof InvalidFormatException ife) {
                        Class<?> targetType = ife.getTargetType();

                        if (targetType != null) {
                                // Handle date-time formatting
                                if (targetType.equals(LocalDateTime.class) || targetType.equals(java.util.Date.class)) {
                                        errorMessage = String.format(
                                                "Invalid date-time format: %s. Expected format: yyyy-MM-dd HH:mm:ss",
                                                ife.getValue());
                                }

                                // Handle ServiceType enum
                                else if (targetType.equals(ServiceType.class)) {
                                        errorMessage = "Invalid service type. Allowed values: VOICE, SMS, USAGE.";
                                }

                                // Handle UnitType enum
                                else if (targetType.equals(UnitType.class)) {
                                        errorMessage = "Invalid unit. Allowed values: SECOND, MINUTE, EVENT, KB, MB, GB, BYTE.";
                                }
                        }
                }

                ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                                request.getDescription(false),
                                HttpStatus.BAD_REQUEST,
                                errorMessage,
                                LocalDateTime.now());

                return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(IllegalArgumentException exception,
                                                                               WebRequest webRequest) {
                ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                        webRequest.getDescription(false),
                        HttpStatus.BAD_REQUEST,
                        exception.getMessage(),
                        LocalDateTime.now());
                return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(ForeignReferenceException.class)
        public ResponseEntity<ErrorResponseDTO> handleForeignReferenceException(
                ForeignReferenceException exception, WebRequest webRequest) {
                ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                        webRequest.getDescription(false),
                        HttpStatus.CONFLICT,
                        exception.getMessage(),
                        LocalDateTime.now());
                return new ResponseEntity<>(errorResponseDTO, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(ResourceConflictException.class)
        public ResponseEntity<ErrorResponseDTO> handleResourceConflictException(
                ResourceConflictException exception, WebRequest webRequest) {
                ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                        webRequest.getDescription(false),
                        HttpStatus.CONFLICT,
                        exception.getMessage(),
                        LocalDateTime.now());
                return new ResponseEntity<>(errorResponseDTO, HttpStatus.CONFLICT);
        }
}