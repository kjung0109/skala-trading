package com.sk.skala.shopapi.dto;

import com.sk.skala.shopapi.domain.Stock;
import lombok.Builder;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;

/**
 * 호가창. 가격대별 잔량을 매도/매수로 나눠 담는다.
 * 매도는 비싼 가격이 위, 매수는 비싼 가격이 위로 오도록 정렬해 실제 HTS 배치와 맞춘다.
 */
@Getter
@Builder
public class OrderBookDto {

    private Long stockId;
    private String stockCode;
    private String stockName;
    private Long currentPrice;
    private List<Level> askLevels;   // 매도 호가
    private List<Level> bidLevels;   // 매수 호가

    @Getter
    @Builder
    public static class Level {
        private Long price;
        private Long quantity;
    }

    public static OrderBookDto of(Stock stock, List<Object[]> asks, List<Object[]> bids) {
        return OrderBookDto.builder()
                .stockId(stock.getId())
                .stockCode(stock.getCode())
                .stockName(stock.getName())
                .currentPrice(stock.getCurrentPrice())
                .askLevels(toLevels(asks, Comparator.comparing(Level::getPrice).reversed()))
                .bidLevels(toLevels(bids, Comparator.comparing(Level::getPrice).reversed()))
                .build();
    }

    private static List<Level> toLevels(List<Object[]> rows, Comparator<Level> comparator) {
        return rows.stream()
                .map(r -> Level.builder()
                        .price(((Number) r[0]).longValue())
                        .quantity(((Number) r[1]).longValue())
                        .build())
                .sorted(comparator)
                .toList();
    }
}
