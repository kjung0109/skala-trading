package com.sk.skala.trading.market;

import com.sk.skala.trading.order.OrderRepository;
import com.sk.skala.trading.order.OrderSide;
import com.sk.skala.trading.stock.Stock;
import com.sk.skala.trading.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 시장 상태 점검.
 *
 * 프로세스가 살아 있고 DB에 붙어 있다고 해서 거래가 가능한 것은 아니다.
 * 한쪽 호가가 비어 있으면 그 방향 시장가 주문은 받을 수 없고, 화면상으로도 고장으로 보인다.
 * 기본 헬스체크로는 이 상태를 잡아낼 수 없어 별도 지표를 만들었다.
 *
 * /actuator/health 에 market 항목으로 나타난다.
 */
@Component("market")
@RequiredArgsConstructor
public class MarketHealthIndicator implements HealthIndicator {

    /** 양쪽 호가가 모두 있어야 정상으로 본다. 이 비율 아래로 떨어지면 경고한다. */
    private static final double HEALTHY_RATIO = 0.8;

    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;

    @Override
    public Health health() {
        List<Stock> stocks = stockRepository.findAll();
        if (stocks.isEmpty()) {
            return Health.down().withDetail("reason", "등록된 종목이 없습니다").build();
        }

        int twoSided = 0;
        StringBuilder thin = new StringBuilder();

        for (Stock stock : stocks) {
            boolean hasAsk = !orderRepository.aggregateOrderBook(stock.getId(), OrderSide.SELL).isEmpty();
            boolean hasBid = !orderRepository.aggregateOrderBook(stock.getId(), OrderSide.BUY).isEmpty();

            if (hasAsk && hasBid) {
                twoSided++;
            } else {
                thin.append(thin.isEmpty() ? "" : ", ")
                        .append(stock.getName())
                        .append(hasAsk ? "(매수없음)" : "(매도없음)");
            }
        }

        double ratio = (double) twoSided / stocks.size();
        Health.Builder builder = ratio >= HEALTHY_RATIO ? Health.up() : Health.down();

        builder.withDetail("종목 수", stocks.size())
                .withDetail("양방향 호가 종목", twoSided)
                .withDetail("호가 충실도", "%.0f%%".formatted(ratio * 100));

        if (!thin.isEmpty()) {
            builder.withDetail("한쪽 호가만 있는 종목", thin.toString());
        }
        return builder.build();
    }
}
