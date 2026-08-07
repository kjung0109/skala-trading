package com.sk.skala.trading.dto;

import com.sk.skala.trading.domain.Order;
import com.sk.skala.trading.domain.OrderSide;
import com.sk.skala.trading.domain.OrderStatus;
import com.sk.skala.trading.domain.OrderType;
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
                .price(o.getPrice())
                .quantity(o.getQuantity())
                .filledQuantity(o.filledQuantity())
                .remainingQuantity(o.getRemainingQuantity())
                .status(o.getStatus())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
