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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 자동 매매 봇.
 *
 * 사람이 아무것도 하지 않아도 호가창이 살아 있고 현재가가 움직이도록 시장을 굴린다.
 * 실제 시장과 같이 역할을 둘로 나눴다.
 *
 *   MAKER  — 현재가 주변에 양방향 호가를 대기만 한다. 절대 걷어가지 않는다.
 *   TAKER  — 걸린 호가를 걷어가 체결을 만들고 현재가를 움직인다.
 *
 * 역할을 계좌 단위로 나눈 이유가 중요하다.
 * 처음에는 한 무리의 봇이 양쪽을 다 맡았는데, 자전거래 방지 때문에
 * 같은 계좌의 매수호가와 매도호가가 서로 만나도 체결되지 않고 그대로 남았다.
 * 그 결과 매수호가가 매도호가보다 높은 "교차된 호가창"이 만들어졌다.
 * 호가를 대는 계좌와 걷어가는 계좌가 다르면 이 상황 자체가 생기지 않는다.
 *
 * app.bot.enabled=false 로 끌 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.bot.enabled", havingValue = "true", matchIfMissing = true)
public class MarketMakerBot {

    /** 호가 공급 전담 계좌 */
    private static final String MAKER = "market01";

    /**
     * 체결 유발 전담 계좌. 매수와 매도를 서로 다른 계좌에 맡긴다.
     *
     * 한 계좌가 양쪽을 다 내면, 다 채우지 못한 잔량이 양쪽에 남는다.
     * 같은 계좌의 매수 잔량과 매도 잔량은 자전거래 방지로 서로 만나지 못해
     * 교차된 호가창(매수호가 > 매도호가)이 그대로 굳어버린다.
     * 방향별로 계좌를 나누면 남은 잔량끼리도 정상적으로 체결된다.
     */
    private static final String TAKER_BUY = "bot01";
    private static final String TAKER_SELL = "bot02";

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;

    @Value("${app.bot.max-quantity:15}")
    private int maxQuantity;

    /** 현재가 기준 위아래로 유지할 호가 단계 수 */
    @Value("${app.bot.min-depth:8}")
    private int minDepth;

    /** 한쪽에 쌓일 수 있는 최대 호가 단계 수. 시세가 흘러가며 남은 호가가 무한정 쌓이는 것을 막는다. */
    @Value("${app.bot.max-depth:28}")
    private int maxDepth;

    /** 한 번에 낼 체결 유발 주문 수 */
    @Value("${app.bot.takers-per-tick:10}")
    private int takersPerTick;

    /** 체결 유발 주문 중 시장가로 낼 비율(%) */
    @Value("${app.bot.market-ratio:35}")
    private int marketRatio;

    // ────────────────────────────────────────────────────────
    // 1. 호가 공급
    // ────────────────────────────────────────────────────────

    /**
     * 현재가 주변 격자에서 비어 있는 가격에만 호가를 채운다.
     *
     * 처음에는 "이미 있는 호가 바깥"에 채웠는데, 걷어가는 쪽은 항상 최우선호가부터
     * 먹기 때문에 안쪽이 비고 바깥만 두꺼워져 스프레드가 계속 벌어졌다.
     * 현재가에서 한 틱씩 떨어진 자리를 기준으로 삼아 빈 자리를 메우면 호가창이 붙어 있는다.
     */
    @Scheduled(fixedDelayString = "${app.bot.quote-interval-ms:500}")
    public void supplyQuotes() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Stock stock : stockRepository.findAll()) {
            try {
                supplyQuotes(stock, random);
            } catch (Exception e) {
                log.debug("[BOT] 호가 공급 실패 {}: {}", stock.getCode(), e.getMessage());
            }
        }
    }

    private void supplyQuotes(Stock stock, ThreadLocalRandom random) {
        Set<Long> askPrices = new HashSet<>(levelPrices(stock, OrderSide.SELL));
        Set<Long> bidPrices = new HashSet<>(levelPrices(stock, OrderSide.BUY));

        long mid = onGrid(stock.getCurrentPrice());
        long tick = tickOf(stock);
        long bestAsk = askPrices.stream().mapToLong(Long::longValue).min().orElse(Long.MAX_VALUE);
        long bestBid = bidPrices.stream().mapToLong(Long::longValue).max().orElse(0L);

        for (int i = 1; i <= minDepth; i++) {
            long askPrice = mid + tick * i;
            long bidPrice = mid - tick * i;

            // 반대쪽 최우선호가를 넘어서면 호가로 남지 않고 그 자리에서 체결된다.
            // 공급자가 체결을 만들어서는 안 되므로 넘지 않는 자리에만 건다.
            if (askPrices.size() < maxDepth && !askPrices.contains(askPrice) && askPrice > bestBid) {
                quote(stock, OrderSide.SELL, askPrice, i, random);
                askPrices.add(askPrice);
            }
            if (bidPrices.size() < maxDepth && !bidPrices.contains(bidPrice)
                    && bidPrice > 0 && bidPrice < bestAsk) {
                quote(stock, OrderSide.BUY, bidPrice, i, random);
                bidPrices.add(bidPrice);
            }
        }
    }

    private void quote(Stock stock, OrderSide side, long price, int step, ThreadLocalRandom random) {
        OrderRequest request = new OrderRequest();
        request.setStockId(stock.getId());
        request.setSide(side);
        request.setType(OrderType.LIMIT);
        request.setPrice(Math.max(price, 1));
        // 멀수록 두껍게 두되, 최우선호가도 주문 한 건에 사라지지 않을 만큼은 채운다.
        // 처음엔 최우선호가에 5~15주만 뒀더니 거래가 몰리는 종목일수록 안쪽이 계속 비어
        // 스프레드가 10틱 넘게 벌어졌다. 실제 시장은 거래가 많은 종목일수록 촘촘하다.
        // 최우선호가가 주문 몇 건에 사라지면 가격이 계속 튄다. 실제로 거래가 많은 종목일수록
        // 최우선호가가 두껍다. 멀수록 더 두껍게 둔다.
        request.setQuantity((long) random.nextInt(60, 120 + step * 30));

        submit(MAKER, request);
    }

    // ────────────────────────────────────────────────────────
    // 2. 체결 유발
    // ────────────────────────────────────────────────────────

    @Scheduled(fixedDelayString = "${app.bot.interval-ms:300}")
    public void generateTrades() {
        List<Stock> stocks = stockRepository.findAll();
        if (stocks.isEmpty()) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < takersPerTick; i++) {
            takeLiquidity(stocks.get(pickWeighted(stocks.size(), random)), random);
        }
    }

    /**
     * 거래가 몰릴 종목을 고른다.
     *
     * 실제 시장의 거래량은 종목마다 크게 다르다. 삼성전자 하나가 하루 거래량의 상당 부분을
     * 차지하는 식이다. 모든 종목을 똑같이 고르면 시세판이 균일하게 움직여 오히려 인공적으로 보인다.
     * 순위에 반비례하는 가중치(1/1, 1/2, 1/3 …)를 줘서 앞쪽 종목에 거래가 몰리게 한다.
     */
    private int pickWeighted(int size, ThreadLocalRandom random) {
        double total = 0;
        for (int i = 1; i <= size; i++) {
            total += 1.0 / i;
        }

        double target = random.nextDouble() * total;
        double sum = 0;
        for (int i = 1; i <= size; i++) {
            sum += 1.0 / i;
            if (target <= sum) {
                return i - 1;
            }
        }
        return size - 1;
    }

    /** 호가를 걷어가 현재가를 움직이는 주문 */
    private void takeLiquidity(Stock stock, ThreadLocalRandom random) {
        OrderRequest request = new OrderRequest();
        request.setStockId(stock.getId());

        OrderSide side = pickSide(stock, random);
        request.setSide(side);
        request.setQuantity((long) random.nextInt(1, maxQuantity + 1));

        if (random.nextInt(100) < marketRatio) {
            // 시장가는 걸려 있던 호가를 소진시키므로 현재가가 실제로 움직인다.
            request.setType(OrderType.MARKET);
        } else {
            // 최우선 반대호가를 그대로 노린다.
            // 처음에는 현재가에서 1~3틱씩 넘겨 냈는데, 그러면 체결마다 가격이 반드시 움직여
            // 1분 등락폭이 1%를 넘었다. 실제 시장은 대부분의 체결이 최우선호가에서 일어나고
            // 그 물량이 다 소진될 때만 가격이 한 칸 움직인다.
            Long best = bestOpposite(stock, side);
            if (best == null) {
                return;   // 상대 호가가 없으면 이번 tick은 건너뛴다
            }
            // 가끔은 한 칸 더 넘겨 내 호가가 실제로 뚫리게 한다.
            long offset = random.nextInt(100) < 15 ? tickOf(stock) : 0;
            long price = best + (side == OrderSide.BUY ? offset : -offset);

            request.setType(OrderType.LIMIT);
            request.setPrice(Math.max(onGrid(price), 1));
        }

        submit(side == OrderSide.BUY ? TAKER_BUY : TAKER_SELL, request);
    }

    /**
     * 매수·매도 방향을 고른다.
     *
     * 완전 무작위로 고르면 가격이 제약 없는 랜덤워크가 되어 하루면 터무니없는 값까지 흘러간다.
     * 실제 장중 가격은 전일 종가 근처를 오간다. 전일 종가에서 멀어질수록 되돌리는 방향이
     * 많이 나오게 해 그 성질을 흉내 낸다.
     */
    private OrderSide pickSide(Stock stock, ThreadLocalRandom random) {
        double reference = stock.getPreviousPrice();
        double deviation = (stock.getCurrentPrice() - reference) / reference;

        // 전일 종가 대비 1% 벗어나면 되돌리는 쪽이 85% 나온다.
        double buyProbability = 0.5 - Math.max(-0.35, Math.min(0.35, deviation * 35));
        return random.nextDouble() < buyProbability ? OrderSide.BUY : OrderSide.SELL;
    }

    /** 반대편 최우선호가. 매수면 최저 매도호가, 매도면 최고 매수호가. */
    private Long bestOpposite(Stock stock, OrderSide side) {
        List<Long> prices = levelPrices(stock, side == OrderSide.BUY ? OrderSide.SELL : OrderSide.BUY);
        if (prices.isEmpty()) {
            return null;
        }
        return side == OrderSide.BUY
                ? prices.stream().mapToLong(Long::longValue).min().getAsLong()
                : prices.stream().mapToLong(Long::longValue).max().getAsLong();
    }

    // ────────────────────────────────────────────────────────

    private void submit(String accountId, OrderRequest request) {
        try {
            orderService.placeOrder(accountId, request);
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

    /**
     * 호가 단위. 한국거래소 규정을 그대로 따른다.
     *
     * 처음에는 현재가의 0.1%로 잡았는데, 그러면 230,764원 같은 호가가 만들어진다.
     * 실제 주식은 가격대별 호가단위의 배수로만 존재하므로 화면이 곧바로 어색해 보인다.
     */
    private long tickOf(Stock stock) {
        return tickOf(stock.getCurrentPrice());
    }

    private long tickOf(long price) {
        if (price < 2_000) return 1;
        if (price < 5_000) return 5;
        if (price < 20_000) return 10;
        if (price < 50_000) return 50;
        if (price < 200_000) return 100;
        if (price < 500_000) return 500;
        return 1_000;
    }

    /** 호가단위 격자에 맞춰 내린다. 격자를 벗어난 가격은 실제로 존재할 수 없다. */
    private long onGrid(long price) {
        long tick = tickOf(price);
        return Math.max(price / tick * tick, tick);
    }
}
