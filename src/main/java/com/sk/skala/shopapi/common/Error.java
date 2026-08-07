package com.sk.skala.shopapi.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 업무 오류 코드.
 *
 * 각 코드가 어떤 HTTP 상태로 나갈지 여기서 함께 정의한다.
 * 코드가 늘어나도 상태 코드 매핑이 예외 처리기 곳곳에 흩어지지 않는다.
 */
@Getter
public enum Error {

    DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 데이터를 찾을 수 없습니다"),
    DATA_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 데이터입니다"),
    INSUFFICIENT_FUNDS(HttpStatus.BAD_REQUEST, "포인트가 부족합니다"),
    INSUFFICIENT_QUANTITY(HttpStatus.BAD_REQUEST, "보유 수량이 부족합니다"),
    ORDER_ALREADY_FILLED(HttpStatus.CONFLICT, "이미 전량 체결된 주문입니다"),
    ORDER_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 주문입니다"),
    NOT_ORDER_OWNER(HttpStatus.FORBIDDEN, "본인의 주문만 처리할 수 있습니다"),
    NO_LIQUIDITY(HttpStatus.BAD_REQUEST, "체결 가능한 호가가 없습니다"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),
    NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다"),
    CONCURRENT_UPDATE(HttpStatus.CONFLICT, "동시에 처리된 요청이 있습니다. 다시 시도해 주세요"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");

    private final HttpStatus status;
    private final String defaultMessage;

    Error(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
