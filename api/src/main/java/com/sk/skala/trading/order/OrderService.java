package com.sk.skala.trading.order;

import com.sk.skala.trading.common.Error;
import com.sk.skala.trading.common.PagedList;
import com.sk.skala.trading.common.Response;
import com.sk.skala.trading.common.SessionHandler;
import com.sk.skala.trading.account.*;
import com.sk.skala.trading.order.*;
import com.sk.skala.trading.stock.Stock;
import com.sk.skala.trading.order.dto.*;
import com.sk.skala.trading.exception.ParameterException;
import com.sk.skala.trading.exception.ResponseException;
import com.sk.skala.trading.account.*;
import com.sk.skala.trading.order.*;
import com.sk.skala.trading.stock.StockRepository;
import com.sk.skala.trading.market.MarketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.sk.skala.trading.account.Account;
import com.sk.skala.trading.account.AccountRepository;
import com.sk.skala.trading.account.Holding;
import com.sk.skala.trading.account.HoldingRepository;
import com.sk.skala.trading.order.dto.OrderBookDto;
import com.sk.skala.trading.order.dto.OrderDto;
import com.sk.skala.trading.order.dto.OrderRequest;
import com.sk.skala.trading.order.dto.OrderResultDto;
import com.sk.skala.trading.order.dto.CandleDto;
import com.sk.skala.trading.order.dto.TradeDto;

/**
 * 주문 접수와 체결(매칭)을 담당한다.
 *
 * 매칭 원칙은 실제 거래소와 같다.
 * - 가격 우선 : 매수는 비싼 주문이, 매도는 싼 주문이 먼저 체결된다
 * - 시간 우선 : 같은 가격이면 먼저 낸 주문이 먼저 체결된다
 * - 체결 가격 : 먼저 호가창에 있던 주문의 가격으로 체결된다
 *              (나중에 온 주문이 유리한 가격을 제시했다면 그 차액은 돌려준다)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    /** 종목 체결 테이프에 내려줄 최근 체결 건수 */
    private static final int RECENT_TRADE_LIMIT = 200;

    /** 캔들을 만들 때 훑을 체결 건수. 넓은 시간 범위를 담아야 추이가 보인다. */
    private static final int CHART_TRADE_SCAN = 12_000;

    /** 화면에 그릴 캔들 개수 상한. 이보다 많으면 봉이 뭉개져 읽히지 않는다. */
    private static final int CHART_MAX_CANDLES = 240;

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final HoldingRepository holdingRepository;
    private final AccountRepository accountRepository;
    private final StockRepository stockRepository;
    private final SessionHandler sessionHandler;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문 접수 후 즉시 매칭을 시도한다.
     *
     * 종목 행에 쓰기 잠금을 건 뒤 시작한다.
     * 매칭은 "호가창 조회 → 체결 대상 판단 → 잔량 갱신"이 원자적이어야 하는데,
     * 두 요청이 같은 종목을 동시에 처리하면 같은 매도 주문을 각각 체결 대상으로 잡아
     * 잔량이 음수가 될 수 있다. 종목 단위로 직렬화해 이를 막는다.
     */
    /** HTTP 요청용. 로그인한 계좌로 주문한다. */
    @Transactional
    public Response placeOrder(OrderRequest request) {
        return placeOrder(sessionHandler.getCurrentAccountId(), request);
    }

    /**
     * 계좌를 직접 지정해 주문한다.
     * 자동 매매 봇처럼 HTTP 요청 컨텍스트가 없는 곳에서 쓴다.
     */
    @Transactional
    public Response placeOrder(String accountId, OrderRequest request) {
        // 1) 종목 잠금 — 이 시점부터 같은 종목의 다른 주문은 대기한다
        Stock stock = stockRepository.findByIdForUpdate(request.getStockId())
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND,
                        "종목을 찾을 수 없습니다: " + request.getStockId()));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND,
                        "계좌를 찾을 수 없습니다: " + accountId));

        return request.isMarketOrder()
                ? placeMarketOrder(request, account, stock)
                : placeLimitOrder(request, account, stock);
    }

    /**
     * 지정가 주문.
     * 매수는 예수금을, 매도는 보유 수량을 주문 시점에 묶는다.
     * 체결될 때가 아니라 주문 시점에 묶어야 잔고보다 많은 주문을 여러 건 낼 수 없다.
     */
    private Response placeLimitOrder(OrderRequest request, Account account, Stock stock) {
        if (request.getPrice() == null) {
            throw new ParameterException("price");
        }

        long price = request.getPrice();
        long quantity = request.getQuantity();

        if (request.getSide() == OrderSide.BUY) {
            account.withdraw(price * quantity);
        } else {
            reserveHolding(account, stock, quantity);
        }

        Order order = orderRepository.save(
                new Order(account, stock, request.getSide(), OrderType.LIMIT, price, quantity));

        List<Trade> trades = match(order, stock, false);

        log.info("[ORDER] {} LIMIT {} {}주 @{} → 체결 {}건, 잔량 {}",
                account.getAccountId(), order.getSide(), quantity, price,
                trades.size(), order.getRemainingQuantity());

        publishOrderBookChange(stock);

        return Response.success(OrderResultDto.of(order, trades));
    }

    /**
     * 시장가 주문.
     *
     * 가격을 지정하지 않으므로 얼마가 필요한지 미리 알 수 없다.
     * 그래서 호가창을 먼저 훑어 실제 체결될 금액을 계산한 뒤 그만큼만 묶는다.
     * 채우지 못한 잔량은 호가창에 남기지 않고 취소한다.
     */
    private Response placeMarketOrder(OrderRequest request, Account account, Stock stock) {
        long quantity = request.getQuantity();

        List<Order> book = (request.getSide() == OrderSide.BUY)
                ? orderRepository.findBestSellOrders(stock.getId(), account.getAccountId())
                : orderRepository.findBestBuyOrders(stock.getId(), account.getAccountId());

        if (book.isEmpty()) {
            throw new ResponseException(Error.NO_LIQUIDITY,
                    "체결 가능한 호가가 없습니다. 지정가 주문을 이용해 주세요");
        }

        if (request.getSide() == OrderSide.BUY) {
            // 실제로 체결될 금액을 먼저 계산한다. 호가를 싼 것부터 훑으며 필요한 만큼만 더한다.
            long need = 0, left = quantity;
            for (Order o : book) {
                if (left == 0) break;
                long q = Math.min(left, o.getRemainingQuantity());
                need += o.getPrice() * q;
                left -= q;
            }
            account.withdraw(need);
        } else {
            reserveHolding(account, stock, quantity);
        }

        // 시장가는 가격 제약이 없다는 뜻으로, 매수는 상한 없음 / 매도는 하한 없음으로 둔다.
        long boundPrice = (request.getSide() == OrderSide.BUY) ? Long.MAX_VALUE : 0L;
        Order order = orderRepository.save(
                new Order(account, stock, request.getSide(), OrderType.MARKET, boundPrice, quantity));

        List<Trade> trades = match(order, stock, true);

        // 못 채운 잔량 정리. 매수는 체결될 금액만 묶었으므로 환급이 없고,
        // 매도는 묶어둔 보유 수량을 돌려준다.
        long unfilled = order.getRemainingQuantity();
        if (unfilled > 0 && request.getSide() == OrderSide.SELL) {
            restoreHolding(account, stock, unfilled);
        }
        order.expireRemaining();

        log.info("[ORDER] {} MARKET {} {}주 → 체결 {}건, 미체결 {}주 취소",
                account.getAccountId(), order.getSide(), quantity, trades.size(), unfilled);

        publishOrderBookChange(stock);

        return Response.success(OrderResultDto.of(order, trades));
    }

    private List<Trade> match(Order order, Stock stock, boolean market) {
        String accountId = order.getAccount().getAccountId();

        List<Order> counterparts;
        if (market) {
            counterparts = (order.getSide() == OrderSide.BUY)
                    ? orderRepository.findBestSellOrders(stock.getId(), accountId)
                    : orderRepository.findBestBuyOrders(stock.getId(), accountId);
        } else {
            counterparts = (order.getSide() == OrderSide.BUY)
                    ? orderRepository.findMatchableSellOrders(stock.getId(), order.getPrice(), accountId)
                    : orderRepository.findMatchableBuyOrders(stock.getId(), order.getPrice(), accountId);
        }

        List<Trade> trades = new ArrayList<>();

        for (Order counter : counterparts) {
            if (order.getRemainingQuantity() == 0) {
                break;
            }

            long tradeQuantity = Math.min(order.getRemainingQuantity(), counter.getRemainingQuantity());
            // 먼저 호가창에 있던 쪽의 가격으로 체결한다.
            long tradePrice = counter.getPrice();

            Order buyOrder = (order.getSide() == OrderSide.BUY) ? order : counter;
            Order sellOrder = (order.getSide() == OrderSide.BUY) ? counter : order;

            settle(buyOrder, sellOrder, stock, tradePrice, tradeQuantity);

            order.fill(tradeQuantity);
            counter.fill(tradeQuantity);

            Trade trade = tradeRepository.save(new Trade(stock, buyOrder, sellOrder, tradePrice, tradeQuantity));
            trades.add(trade);

            stock.applyTradePrice(tradePrice);

            // 커밋된 뒤에만 화면으로 나가도록 트랜잭션 이벤트로 발행한다.
            eventPublisher.publishEvent(MarketEvent.trade(
                    stock.getId(), stock.getCode(), stock.getName(),
                    tradePrice, tradeQuantity, order.getSide().name()));
        }
        return trades;
    }

    /** 체결 정산: 매수자에게 주식을, 매도자에게 대금을 넘긴다. */
    private void settle(Order buyOrder, Order sellOrder, Stock stock, long tradePrice, long quantity) {
        Account buyer = buyOrder.getAccount();
        Account seller = sellOrder.getAccount();

        // 지정가 매수는 자기 지정가로 예수금을 묶어뒀다. 더 싸게 체결됐다면 차액을 돌려준다.
        // 시장가는 실제 체결될 금액만 묶었으므로 환급 대상이 아니다.
        if (buyOrder.getType() == OrderType.LIMIT) {
            long refund = (buyOrder.getPrice() - tradePrice) * quantity;
            if (refund > 0) {
                buyer.deposit(refund);
            }
        }

        // 매도자는 주문 시 보유 수량을 이미 차감했으므로 대금만 받는다.
        seller.deposit(tradePrice * quantity);

        holdingRepository.findByAccountAndStock(buyer, stock)
                .ifPresentOrElse(
                        h -> h.addBuy(quantity, tradePrice),
                        () -> holdingRepository.save(new Holding(buyer, stock, quantity, tradePrice)));
    }

    private void publishOrderBookChange(Stock stock) {
        eventPublisher.publishEvent(MarketEvent.orderBook(
                stock.getId(), stock.getCode(), stock.getName(), stock.getCurrentPrice()));
    }

    /** 묶어둔 보유 수량을 되돌린다. */
    private void restoreHolding(Account account, Stock stock, long quantity) {
        holdingRepository.findByAccountAndStock(account, stock)
                .ifPresentOrElse(
                        h -> h.addBuy(quantity, h.getAveragePrice()),
                        () -> holdingRepository.save(
                                new Holding(account, stock, quantity, stock.getCurrentPrice())));
    }

    /** 매도 주문 시 보유 수량을 묶는다. 부족하면 주문 자체가 거부된다. */
    private void reserveHolding(Account account, Stock stock, long quantity) {
        Holding holding = holdingRepository.findByAccountAndStock(account, stock)
                .orElseThrow(() -> new ResponseException(Error.INSUFFICIENT_QUANTITY,
                        "보유하지 않은 종목입니다: " + stock.getName()));

        if (holding.getQuantity() < quantity) {
            throw new ResponseException(Error.INSUFFICIENT_QUANTITY,
                    "보유 수량이 부족합니다. 보유: %d주, 주문: %d주".formatted(holding.getQuantity(), quantity));
        }
        holding.reduce(quantity);

        // 전량 매도해 0주가 되면 보유 목록에서 지운다.
        // 남겨두면 자산 화면에 "0주 보유" 항목이 계속 쌓인다.
        if (holding.isEmpty()) {
            holdingRepository.delete(holding);
        }
    }

    /**
     * 주문 취소. 미체결 잔량에 대해 묶어둔 자원을 돌려준다.
     * 이미 체결된 부분은 되돌리지 않는다.
     */
    @Transactional
    public Response cancelOrder(Long orderId) {
        String accountId = sessionHandler.getCurrentAccountId();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "주문을 찾을 수 없습니다: " + orderId));

        if (!order.getAccount().getAccountId().equals(accountId)) {
            throw new ResponseException(Error.NOT_ORDER_OWNER, "본인의 주문만 취소할 수 있습니다");
        }

        long remaining = order.getRemainingQuantity();
        order.cancel();   // 이미 체결·취소된 주문이면 여기서 예외

        if (order.getSide() == OrderSide.BUY) {
            order.getAccount().deposit(order.getPrice() * remaining);
        } else {
            Stock stock = order.getStock();
            holdingRepository.findByAccountAndStock(order.getAccount(), stock)
                    .ifPresentOrElse(
                            h -> h.addBuy(remaining, h.getAveragePrice()),
                            () -> holdingRepository.save(
                                    new Holding(order.getAccount(), stock, remaining, stock.getCurrentPrice())));
        }

        log.info("[CANCEL] {} 주문#{} 잔량 {}주 반환", accountId, orderId, remaining);
        publishOrderBookChange(order.getStock());
        return Response.success(OrderDto.from(order));
    }

    /** 호가창 */
    public Response getOrderBook(Long stockId) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "종목을 찾을 수 없습니다: " + stockId));

        return Response.success(OrderBookDto.of(
                stock,
                orderRepository.aggregateOrderBook(stockId, OrderSide.SELL),
                orderRepository.aggregateOrderBook(stockId, OrderSide.BUY)));
    }

    /** 내 주문 목록 */
    public Response getMyOrders(int offset, int count) {
        String accountId = sessionHandler.getCurrentAccountId();
        Page<Order> page = orderRepository.findByAccountId(accountId,
                PageRequest.of(offset / Math.max(count, 1), count));
        return Response.success(PagedList.of(page, offset, count, OrderDto::from));
    }

    /** 내 체결 내역 */
    public Response getMyTrades(int offset, int count) {
        String accountId = sessionHandler.getCurrentAccountId();
        Page<Trade> page = tradeRepository.findByAccountId(accountId,
                PageRequest.of(offset / Math.max(count, 1), count));
        return Response.success(PagedList.of(page, offset, count, t -> TradeDto.from(t, accountId)));
    }

    /**
     * 종목 가격 추이(캔들).
     *
     * 체결을 지정한 시간 구간으로 접어 구간마다 시가·고가·저가·종가·거래량을 만든다.
     * 실제 증권 화면과 같은 형태로 보이게 하려면 선 하나가 아니라 이 다섯 값이 필요하다.
     *
     * @param intervalSeconds 캔들 하나가 담는 시간(초)
     */
    public Response getCandles(Long stockId, int intervalSeconds) {
        int interval = Math.max(intervalSeconds, 1);

        List<Object[]> rows = tradeRepository.findRecentTradePoints(
                stockId, PageRequest.of(0, CHART_TRADE_SCAN));

        // 조회 결과가 최신순이므로 뒤에서부터 훑어 시간순으로 쌓는다.
        Map<LocalDateTime, long[]> buckets = new LinkedHashMap<>();
        for (int i = rows.size() - 1; i >= 0; i--) {
            LocalDateTime tradedAt = (LocalDateTime) rows.get(i)[0];
            long price = ((Number) rows.get(i)[1]).longValue();
            long quantity = ((Number) rows.get(i)[2]).longValue();

            LocalDateTime bucket = truncate(tradedAt, interval);
            long[] ohlcv = buckets.get(bucket);

            if (ohlcv == null) {
                // [시가, 고가, 저가, 종가, 거래량]
                buckets.put(bucket, new long[]{price, price, price, price, quantity});
            } else {
                ohlcv[1] = Math.max(ohlcv[1], price);
                ohlcv[2] = Math.min(ohlcv[2], price);
                ohlcv[3] = price;
                ohlcv[4] += quantity;
            }
        }

        List<CandleDto> candles = buckets.entrySet().stream()
                .map(e -> CandleDto.builder()
                        .time(e.getKey())
                        .open(e.getValue()[0])
                        .high(e.getValue()[1])
                        .low(e.getValue()[2])
                        .close(e.getValue()[3])
                        .volume(e.getValue()[4])
                        .build())
                .toList();

        // 구간이 짧으면 캔들이 너무 많아진다. 최근 것부터 상한만큼만 남긴다.
        if (candles.size() > CHART_MAX_CANDLES) {
            candles = candles.subList(candles.size() - CHART_MAX_CANDLES, candles.size());
        }
        return Response.success(candles);
    }

    /** 시각을 구간 시작점으로 내린다. (예: 5초 구간에서 12:00:07 → 12:00:05) */
    private LocalDateTime truncate(LocalDateTime time, int intervalSeconds) {
        long epoch = time.toEpochSecond(ZoneOffset.UTC);
        return LocalDateTime.ofEpochSecond(epoch - Math.floorMod(epoch, intervalSeconds), 0, ZoneOffset.UTC);
    }

    /** 종목별 최근 체결 내역 */
    public Response getStockTrades(Long stockId) {
        return Response.success(
                tradeRepository.findByStockId(stockId, PageRequest.of(0, RECENT_TRADE_LIMIT)).stream()
                        .map(t -> TradeDto.from(t, null))
                        .toList());
    }
}
