package com.sk.skala.trading.order.dto;

import com.sk.skala.trading.order.OrderSide;
import com.sk.skala.trading.order.Trade;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TradeDto {

    private Long tradeId;
    private String stockCode;
    private String stockName;
    private Long price;
    private Long quantity;
    private Long amount;
    /** 조회한 계좌 기준 매수/매도 구분. 종목별 조회처럼 주체가 없으면 null */
    private OrderSide mySide;
    private LocalDateTime tradedAt;

    public static TradeDto from(Trade t, String accountId) {
        OrderSide mySide = null;
        if (accountId != null) {
            mySide = t.getBuyOrder().getAccount().getAccountId().equals(accountId)
                    ? OrderSide.BUY : OrderSide.SELL;
        }
        return TradeDto.builder()
                .tradeId(t.getId())
                .stockCode(t.getStock().getCode())
                .stockName(t.getStock().getName())
                .price(t.getPrice())
                .quantity(t.getQuantity())
                .amount(t.amount())
                .mySide(mySide)
                .tradedAt(t.getTradedAt())
                .build();
    }
}
