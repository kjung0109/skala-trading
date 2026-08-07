package com.sk.skala.shopapi.exception;

import com.sk.skala.shopapi.common.Error;
import lombok.Getter;

/**
 * 업무 규칙 위반이나 데이터 문제를 나타내는 예외.
 * 어떤 Error 코드인지만 담고, HTTP 상태 변환은 GlobalExceptionHandler가 맡는다.
 */
@Getter
public class ResponseException extends RuntimeException {

    private final Error error;

    public ResponseException(Error error) {
        super(error.getDefaultMessage());
        this.error = error;
    }

    public ResponseException(Error error, String message) {
        super(message);
        this.error = error;
    }
}
