package com.sk.skala.trading.dto;

import com.sk.skala.trading.domain.Order;
import com.sk.skala.trading.domain.Trade;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 주문 접수 결과. 그 자리에서 몇 주가 체결됐는지 함께 알려준다. */
@Getter
@Builder
public class OrderResultDto {

    private OrderDto order;
    private long tradedQuantity;
    private long tradedAmount;
    private List<TradeDto> trades;
    private String message;

    public static OrderResultDto of(Order order, List<Trade> trades) {
        long qty = trades.stream().mapToLong(Trade::getQuantity).sum();
        long amount = trades.stream().mapToLong(Trade::amount).sum();

        String message = switch (order.getStatus()) {
            case FILLED -> "전량 체결되었습니다";
            case PARTIALLY_FILLED -> "%d주 체결, %d주는 호가창에 등록되었습니다"
                    .formatted(qty, order.getRemainingQuantity());
            // 시장가는 호가창에 남기지 않으므로 못 채운 잔량은 소멸한다
            case EXPIRED -> qty > 0
                    ? "%d주 체결, 호가 물량이 부족해 잔량 %d주는 취소되었습니다"
                            .formatted(qty, order.getRemainingQuantity())
                    : "체결 가능한 호가가 없어 주문이 취소되었습니다";
            default -> "체결 가능한 상대 주문이 없어 호가창에 등록되었습니다";
        };

        return OrderResultDto.builder()
                .order(OrderDto.from(order))
                .tradedQuantity(qty)
                .tradedAmount(amount)
                .trades(trades.stream().map(t -> TradeDto.from(t, null)).toList())
                .message(message)
                .build();
    }
}
