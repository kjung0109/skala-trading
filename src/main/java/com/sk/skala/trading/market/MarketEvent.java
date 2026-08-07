package com.sk.skala.trading.market;

import java.time.LocalDateTime;

/**
 * 시장에서 일어난 일을 화면에 알리기 위한 이벤트.
 *
 * 엔티티가 아니라 값 객체로 둔다. 이벤트는 발행 시점의 사실을 담은 스냅샷이므로
 * 나중에 엔티티가 바뀌어도 이미 보낸 내용이 달라지면 안 된다.
 */
public record MarketEvent(
        Type type,
        Long stockId,
        String stockCode,
        String stockName,
        Long price,        // 체결가 또는 현재가
        Long quantity,     // 체결 수량 (호가 변경 이벤트에서는 null)
        String side,       // 체결을 유발한 주문 방향
        LocalDateTime at
) {
    public enum Type {
        TRADE,       // 체결 발생
        ORDER_BOOK   // 호가 변경 (주문 접수·취소)
    }

    public static MarketEvent trade(Long stockId, String code, String name,
                                    long price, long quantity, String side) {
        return new MarketEvent(Type.TRADE, stockId, code, name, price, quantity, side, LocalDateTime.now());
    }

    public static MarketEvent orderBook(Long stockId, String code, String name, long price) {
        return new MarketEvent(Type.ORDER_BOOK, stockId, code, name, price, null, null, LocalDateTime.now());
    }
}
