package com.sk.skala.trading.market;

import com.sk.skala.trading.order.OrderRepository;
import com.sk.skala.trading.order.OrderService;
import com.sk.skala.trading.order.OrderSide;
import com.sk.skala.trading.order.OrderType;
import com.sk.skala.trading.order.dto.OrderRequest;
import com.sk.skala.trading.stock.Stock;
import com.sk.skala.trading.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 자동 매매 봇.
 *
 * 사람이 아무것도 하지 않아도 호가창이 살아 있고 현재가가 움직이도록 두 가지 역할을 나눠 맡는다.
 *
 *  1. 호가 공급 — 양쪽 호가가 얕아진 종목에만 지정가를 채워 넣는다.
 *  2. 체결 유발 — 몇 건은 시장가·공격적 지정가로 내 실제로 체결이 일어나게 한다.
 *
 * 처음에는 2번만 있었는데, 공격적 주문이 호가를 계속 걷어가기만 해서
 * 몇 분 만에 종목 절반의 매도호가가 0단계가 됐다. 한쪽이 빈 호가창은
 * 화면상 고장으로 보이고 그쪽으로는 시장가 주문도 받을 수 없다.
 * 그래서 소비하는 쪽과 공급하는 쪽을 분리했다.
 *
 * 같은 계좌끼리는 체결되지 않으므로(자전거래 방지) 봇 계좌를 둘로 나눠 서로 상대가 되게 했다.
 * app.bot.enabled=false 로 끌 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.bot.enabled", havingValue = "true", matchIfMissing = true)
public class MarketMakerBot {

    private static final String[] BOTS = {"bot01", "bot02"};

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;

    @Value("${app.bot.max-quantity:15}")
    private int maxQuantity;

    /** 한쪽에 유지할 최소 호가 단계 수. 이보다 얕아지면 채운다. */
    @Value("${app.bot.min-depth:6}")
    private int minDepth;

    /** 한 번에 낼 체결 유발 주문 수 */
    @Value("${app.bot.takers-per-tick:3}")
    private int takersPerTick;

    /** 체결 유발 주문 중 시장가로 낼 비율(%) */
    @Value("${app.bot.market-ratio:35}")
    private int marketRatio;

    @Scheduled(fixedDelayString = "${app.bot.interval-ms:1200}")
    public void trade() {
        List<Stock> stocks = stockRepository.findAll();
        if (stocks.isEmpty()) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 1. 얕아진 호가부터 채운다. 채우기 전에 걷어가면 빈 호가창이 그대로 노출된다.
        for (Stock stock : stocks) {
            replenish(stock, random);
        }

        // 2. 한 번에 여러 종목을 건드려야 화면 전체가 살아 움직인다.
        //    한 종목씩만 처리하면 12종목 기준 한 종목이 몇 분에 한 번 바뀐다.
        for (int i = 0; i < takersPerTick; i++) {
            placeTakerOrder(stocks.get(random.nextInt(stocks.size())), random);
        }
    }

    /**
     * 부족한 쪽 호가만 채운다.
     *
     * 이미 minDepth를 채운 쪽은 건드리지 않으므로 주문이 무한정 쌓이지 않는다.
     */
    private void replenish(Stock stock, ThreadLocalRandom random) {
        List<Long> askPrices = levelPrices(stock, OrderSide.SELL);
        List<Long> bidPrices = levelPrices(stock, OrderSide.BUY);

        long tick = tickOf(stock);
        long price = stock.getCurrentPrice();
        long bestAsk = askPrices.stream().mapToLong(Long::longValue).min().orElse(price);
        long bestBid = bidPrices.stream().mapToLong(Long::longValue).max().orElse(price);

        // 이미 있는 호가보다 한 칸 바깥에서 시작한다.
        // 같은 가격에 또 내면 단계 수가 늘지 않아 매 tick마다 잔량만 불어난다.
        // 반대편 최우선호가를 넘지 않아야 체결되지 않고 호가로 쌓인다.
        long askFrom = Math.max(askPrices.isEmpty() ? price : Collections.max(askPrices), bestBid) + tick;
        long bidFrom = Math.min(bidPrices.isEmpty() ? price : Collections.min(bidPrices), bestAsk) - tick;

        for (int i = 0; i < minDepth - askPrices.size(); i++) {
            quote(stock, OrderSide.SELL, askFrom + tick * i, random);
        }
        for (int i = 0; i < minDepth - bidPrices.size(); i++) {
            quote(stock, OrderSide.BUY, bidFrom - tick * i, random);
        }
    }

    private void quote(Stock stock, OrderSide side, long price, ThreadLocalRandom random) {
        OrderRequest request = new OrderRequest();
        request.setStockId(stock.getId());
        request.setSide(side);
        request.setType(OrderType.LIMIT);
        request.setPrice(Math.max(price, 1));
        // 호가가 멀수록 잔량을 두껍게 둔다. 실제 호가창의 모양과 같다.
        request.setQuantity((long) random.nextInt(5, maxQuantity * 2 + 1));
        submit(request, random);
    }

    /** 호가를 걷어가 현재가를 움직이는 주문 */
    private void placeTakerOrder(Stock stock, ThreadLocalRandom random) {
        OrderRequest request = new OrderRequest();
        request.setStockId(stock.getId());
        request.setSide(random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL);
        request.setQuantity((long) random.nextInt(1, maxQuantity + 1));

        if (random.nextInt(100) < marketRatio) {
            // 시장가는 걸려 있던 호가를 소진시키므로 현재가가 실제로 움직인다.
            request.setType(OrderType.MARKET);
        } else {
            // 지정가라도 상대 호가를 넘겨서 내야 체결된다.
            long offset = tickOf(stock) * random.nextInt(1, 4);
            long price = stock.getCurrentPrice() + (request.getSide() == OrderSide.BUY ? offset : -offset);
            request.setType(OrderType.LIMIT);
            request.setPrice(Math.max(price, 1));
        }

        submit(request, random);
    }

    private void submit(OrderRequest request, ThreadLocalRandom random) {
        try {
            orderService.placeOrder(BOTS[random.nextInt(BOTS.length)], request);
        } catch (Exception e) {
            // 잔고·보유수량 부족이나 유동성 없음은 정상적인 거절이다.
            // 봇이 멈추면 안 되므로 로그만 남기고 넘어간다.
            log.debug("[BOT] 주문 거절: {}", e.getMessage());
        }
    }

    private List<Long> levelPrices(Stock stock, OrderSide side) {
        return orderRepository.aggregateOrderBook(stock.getId(), side).stream()
                .map(row -> ((Number) row[0]).longValue())
                .toList();
    }

    /** 호가 단위. 종목 가격대에 비례시켜 저가주와 고가주가 같은 폭으로 움직이게 한다. */
    private long tickOf(Stock stock) {
        return Math.max(stock.getCurrentPrice() / 1000, 1);
    }
}
