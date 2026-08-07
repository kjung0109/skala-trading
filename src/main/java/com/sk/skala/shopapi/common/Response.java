package com.sk.skala.shopapi.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 모든 API의 공통 응답 형식.
 *
 * 성공과 실패를 같은 껍데기로 내려주면 클라이언트가 응답을 한 가지 방식으로만 다루면 된다.
 * result 로 성공 여부를 판단하고, 실패일 때만 error 를 읽으면 된다.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {

    public static final String SUCCESS = "success";
    public static final String FAIL = "fail";

    private final String result;
    private final Object body;
    private final ErrorDetail error;

    private Response(String result, Object body, ErrorDetail error) {
        this.result = result;
        this.body = body;
        this.error = error;
    }

    public static Response success() {
        return new Response(SUCCESS, null, null);
    }

    public static Response success(Object body) {
        return new Response(SUCCESS, body, null);
    }

    public static Response fail(Error error, String message) {
        return new Response(FAIL, null, new ErrorDetail(error.name(), message));
    }

    public static Response fail(Error error, String message, Object body) {
        return new Response(FAIL, body, new ErrorDetail(error.name(), message));
    }

    @Getter
    public static class ErrorDetail {
        private final String code;
        private final String message;

        ErrorDetail(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
