package com.sk.skala.trading.order;

/**
 * 주문 유형.
 *
 * LIMIT(지정가)  : 가격을 지정한다. 조건이 맞지 않으면 호가창에 남아 기다린다.
 * MARKET(시장가) : 가격을 지정하지 않는다. 현재 걸려 있는 호가로 즉시 체결하고,
 *                 물량이 모자라 채우지 못한 잔량은 남기지 않고 취소한다.
 *                 (실제 거래소도 시장가 주문은 호가창에 등록하지 않는다)
 */
public enum OrderType {
    LIMIT, MARKET
}
