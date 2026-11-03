package cn.whaa.blog.api.web;

import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.Result;
import cn.whaa.blog.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBiz(BizException ex) {
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleValidation(Exception ex) {
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "validation failed");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "uploaded file exceeds the allowed size limit");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleOther(Exception ex) {
        log.error("Unhandled error", ex);
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }
}
