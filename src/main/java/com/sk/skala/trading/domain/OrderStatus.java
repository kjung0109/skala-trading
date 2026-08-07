package com.sk.skala.trading.domain;

/**
 * 주문 상태.
 * OPEN(미체결) → PARTIALLY_FILLED(부분체결) → FILLED(전량체결)
 * 체결 전이면 언제든 CANCELLED로 갈 수 있다.
 * 시장가 주문은 호가창에 남기지 않으므로, 못 채운 잔량은 EXPIRED로 소멸시킨다.
 */
public enum OrderStatus {

    OPEN,              // 미체결, 호가창에 대기
    PARTIALLY_FILLED,  // 일부 체결, 잔량은 호가창에 대기
    FILLED,            // 전량 체결
    CANCELLED,         // 사용자가 취소
    EXPIRED;           // 시장가 주문의 미체결 잔량 소멸

    /** 호가창에 남아 매칭 대상이 되는 상태인가 */
    public boolean isActive() {
        return this == OPEN || this == PARTIALLY_FILLED;
    }
}
