package com.sk.skala.shopapi.dto;

import com.sk.skala.shopapi.domain.Holding;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HoldingDto {

    private Long stockId;
    private String stockCode;
    private String stockName;
    private Long quantity;
    private Long averagePrice;
    private Long currentPrice;
    private Long valuation;       // 평가 금액
    private Long profitLoss;      // 평가 손익
    private Double profitLossRate;

    public static HoldingDto from(Holding h) {
        long current = h.getStock().getCurrentPrice();
        long valuation = current * h.getQuantity();
        long cost = h.getAveragePrice() * h.getQuantity();
        long profitLoss = valuation - cost;

        return HoldingDto.builder()
                .stockId(h.getStock().getId())
                .stockCode(h.getStock().getCode())
                .stockName(h.getStock().getName())
                .quantity(h.getQuantity())
                .averagePrice(h.getAveragePrice())
                .currentPrice(current)
                .valuation(valuation)
                .profitLoss(profitLoss)
                .profitLossRate(cost == 0 ? 0.0 : Math.round(profitLoss * 10000.0 / cost) / 100.0)
                .build();
    }
}
