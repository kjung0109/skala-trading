package com.sk.skala.trading.order.dto;

import com.sk.skala.trading.order.Order;
import com.sk.skala.trading.order.OrderSide;
import com.sk.skala.trading.order.OrderStatus;
import com.sk.skala.trading.order.OrderType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderDto {

    private Long orderId;
    private String stockCode;
    private String stockName;
    private OrderSide side;
    private OrderType type;
    private Long price;
    private Long quantity;
    private Long filledQuantity;
    private Long remainingQuantity;
    private OrderStatus status;
    private LocalDateTime createdAt;

    public static OrderDto from(Order o) {
        return OrderDto.builder()
                .orderId(o.getId())
                .stockCode(o.getStock().getCode())
                .stockName(o.getStock().getName())
                .side(o.getSide())
                .type(o.getType())
                // 시장가는 내부적으로 "상한 없음"을 Long.MAX_VALUE로 표현한다.
                // 그 값이 그대로 나가면 화면에 9223372036854775807이 찍히므로 감춘다.
                // 실제 체결가는 체결 내역에 남는다.
                .price(o.getType() == OrderType.MARKET ? null : o.getPrice())
                .quantity(o.getQuantity())
                .filledQuantity(o.filledQuantity())
                .remainingQuantity(o.getRemainingQuantity())
                .status(o.getStatus())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
