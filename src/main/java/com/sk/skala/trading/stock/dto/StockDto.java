package com.sk.skala.trading.stock.dto;

import com.sk.skala.trading.stock.Stock;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockDto {

    private Long id;
    private String code;
    private String name;
    private Long currentPrice;
    private Long previousPrice;
    private Long change;        // 전일 대비
    private Double changeRate;  // 등락률

    public static StockDto from(Stock s) {
        long change = s.getCurrentPrice() - s.getPreviousPrice();
        return StockDto.builder()
                .id(s.getId())
                .code(s.getCode())
                .name(s.getName())
                .currentPrice(s.getCurrentPrice())
                .previousPrice(s.getPreviousPrice())
                .change(change)
                .changeRate(s.getPreviousPrice() == 0 ? 0.0
                        : Math.round(change * 10000.0 / s.getPreviousPrice()) / 100.0)
                .build();
    }
}
