package com.sk.skala.trading.market;

import com.sk.skala.trading.order.OrderSide;
import com.sk.skala.trading.order.OrderType;
import com.sk.skala.trading.order.OrderService;
import com.sk.skala.trading.order.dto.OrderRequest;
import com.sk.skala.trading.stock.Stock;
import com.sk.skala.trading.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 자동 매매 봇.
 *
 * 두 계좌가 서로 주문을 내며 시장을 움직인다.
 * 호가창이 정지 화면이면 시스템이 살아 있는지 알 수 없으므로,
 * 사람이 아무것도 하지 않아도 체결이 일어나고 현재가가 변하도록 한다.
 *
 * 같은 계좌끼리는 체결되지 않게 막아뒀기 때문에(자전거래 방지)
 * 봇 계좌를 둘로 나눠 서로 상대가 되게 했다.
 *
 * app.bot.enabled=false 로 끌 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.bot.enabled", havingValue = "true", matchIfMissing = true)
public class MarketMakerBot {

    private static final String[] BOTS = {"bot01", "bot02"};

    private final OrderService orderService;
    private final StockRepository stockRepository;

    @Value("${app.bot.max-quantity:15}")
    private int maxQuantity;

    @Scheduled(fixedDelayString = "${app.bot.interval-ms:3000}")
    public void trade() {
        List<Stock> stocks = stockRepository.findAll();
        if (stocks.isEmpty()) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Stock stock = stocks.get(random.nextInt(stocks.size()));
        String bot = BOTS[random.nextInt(BOTS.length)];

        OrderRequest request = new OrderRequest();
        request.setStockId(stock.getId());
        request.setSide(random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL);
        request.setQuantity((long) random.nextInt(1, maxQuantity + 1));

        // 10번에 2번 정도는 시장가를 낸다. 그래야 걸려 있던 호가가 소진되며 현재가가 움직인다.
        if (random.nextInt(10) < 2) {
            request.setType(OrderType.MARKET);
        } else {
            request.setType(OrderType.LIMIT);
            // 현재가 ±0.3% 안에서 가격을 흩뿌려 호가창이 여러 단계로 쌓이게 한다.
            long tick = Math.max(stock.getCurrentPrice() / 1000, 1);
            long offset = tick * random.nextInt(-3, 4);
            request.setPrice(Math.max(stock.getCurrentPrice() + offset, 1));
        }

        try {
            orderService.placeOrder(bot, request);
        } catch (Exception e) {
            // 잔고·보유수량 부족이나 유동성 없음은 정상적인 거절이다.
            // 봇이 멈추면 안 되므로 로그만 남기고 넘어간다.
            log.debug("[BOT] {} 주문 거절: {}", bot, e.getMessage());
        }
    }
}
