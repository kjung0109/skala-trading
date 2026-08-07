package com.sk.skala.trading.exception;

import com.sk.skala.trading.common.Error;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 입력값이 비었거나 형식이 맞지 않을 때 던진다.
 * 어떤 항목이 문제인지 함께 담아, 클라이언트가 어느 필드를 고쳐야 하는지 알 수 있게 한다.
 */
@Getter
public class ParameterException extends RuntimeException {

    private final Error error = Error.INVALID_PARAMETER;
    private final List<String> parameters;

    public ParameterException(String... parameters) {
        super("입력값이 올바르지 않습니다: " + String.join(", ", parameters));
        this.parameters = Arrays.asList(parameters);
    }
}
