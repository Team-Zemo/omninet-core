package org.zemo.omninet.notes.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.zemo.omninet.notes.util.CommonUtil;

import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<?> handleException(Exception e) {
//        log.error("GlobalExceptionHandler :: handleException ::"+e.getMessage());
//        return  new ResponseEntity<>("error hai :"+e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();

        Map<String, Object> error = new LinkedHashMap<>();

        allErrors.forEach(objectError -> {
            String msg = objectError.getDefaultMessage();
            String field = ((FieldError) (objectError)).getField();
            error.put(field, msg);
        });

        log.error("MethodArgumentNotValidException :: " + e.getMessage());

        return CommonUtil.createErrorResponse(error, HttpStatus.BAD_REQUEST);
//       return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<?> handleNullPointerException(NullPointerException e) {
        log.error("GlobalExceptionHandler :: handleNullPointerException ::" + e.getMessage());

        return CommonUtil.createErrorResponseMessage("error null pointer  :" + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//    return  new ResponseEntity<>("error null pointer  :"+e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFoundException(Exception e) {
        log.error("GlobalExceptionHandler :: handleResourceNotFoundException ::" + e.getMessage());

        return CommonUtil.createErrorResponseMessage("error hai :" + e.getMessage(), HttpStatus.NOT_FOUND);
//        return  new ResponseEntity<>("error hai :"+e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handleValidationException(ValidationException e) {
        log.error("GlobalExceptionHandler :: handleValidationException ::" + e.getError());

        return CommonUtil.createErrorResponse(e.getError(), HttpStatus.NOT_FOUND);
//        return  new ResponseEntity<>(e.getError(), HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(ExistDataException.class)
    public ResponseEntity<?> handleExistDataException(ExistDataException e) {
        log.error("GlobalExceptionHandler :: handleExistDataException ::" + e.getMessage());

        return CommonUtil.createErrorResponseMessage(e.getMessage(), HttpStatus.CONFLICT);
//        return  new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
    }

    //    HttpMessageNotReadableException
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("GlobalExceptionHandler :: handleHttpMessageNotReadableException ::" + e.getMessage());

        return CommonUtil.createErrorResponseMessage(e.getMessage(), HttpStatus.BAD_REQUEST);
//    return  new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<?> handleFileNotFoundException(FileNotFoundException e) {
        log.error("GlobalExceptionHandler :: handleFileNotFoundException ::" + e.getMessage());

        return CommonUtil.createErrorResponseMessage(e.getMessage(), HttpStatus.NOT_FOUND);
//    return  new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("GlobalExceptionHandler :: handleIllegalArgumentException ::" + e.getMessage());
        return CommonUtil.createErrorResponseMessage(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SuccessException.class)
    public ResponseEntity<?> handleSuccessException(SuccessException e) {
        log.error("GlobalExceptionHandler :: handleSuccessException ::" + e.getMessage());

        return CommonUtil.createBuildResponseMessage(e.getMessage(), HttpStatus.OK);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException e) {
        log.error("GlobalExceptionHandler :: handleBadCredentialsException ::" + e.getMessage());

        return CommonUtil.createErrorResponseMessage(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    //

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException e) {
        return CommonUtil.createErrorResponseMessage("allowed nhi hai", HttpStatus.FORBIDDEN);
    }


}
