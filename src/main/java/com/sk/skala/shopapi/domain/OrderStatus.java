package com.sk.skala.shopapi.domain;

/**
 * 주문 상태.
 * OPEN(미체결) → PARTIALLY_FILLED(부분체결) → FILLED(전량체결)
 * 체결 전이면 언제든 CANCELLED로 갈 수 있다.
 */
public enum OrderStatus {
    OPEN, PARTIALLY_FILLED, FILLED, CANCELLED;

    /** 호가창에 남아 매칭 대상이 되는 상태인가 */
    public boolean isActive() {
        return this == OPEN || this == PARTIALLY_FILLED;
    }
}
