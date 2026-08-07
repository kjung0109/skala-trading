package com.sk.skala.trading.order;

import com.sk.skala.trading.account.Account;
import com.sk.skala.trading.account.AccountRepository;
import com.sk.skala.trading.account.Holding;
import com.sk.skala.trading.account.HoldingRepository;
import com.sk.skala.trading.common.Response;
import com.sk.skala.trading.exception.ResponseException;
import com.sk.skala.trading.order.dto.OrderRequest;
import com.sk.skala.trading.order.dto.OrderResultDto;
import com.sk.skala.trading.stock.Stock;
import com.sk.skala.trading.stock.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 매칭 엔진 검증.
 *
 * 이 프로젝트의 핵심은 "주문이 만나 가격이 만들어지는 것"이므로,
 * 가격 우선 · 시간 우선 · 부분 체결 · 가격 개선 환불이 규칙대로 동작하는지 확인한다.
 *
 * 봇과 초기 호가 시딩은 test 프로파일에서 꺼둔다(application-test.yml).
 * 켜져 있으면 테스트가 만든 호가가 봇에게 먹혀 결과가 매번 달라진다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderMatchingTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private HoldingRepository holdingRepository;
    @Autowired private StockRepository stockRepository;

    private Stock stock;

    /** 매도자는 market01(보유 10,000주), 매수자는 trader01(예수금 1억). */
    private static final String SELLER = "market01";
    private static final String BUYER = "trader01";

    @BeforeEach
    void setUp() {
        stock = stockRepository.findAll().get(0);
    }

    @Test
    @DisplayName("체결 상대가 없으면 호가창에 등록만 된다")
    void restsOnBook_whenNoCounterparty() {
        Response response = orderService.placeOrder(BUYER, buy(stock, 1_000L, 5L));
        OrderResultDto result = body(response);

        assertThat(result.getTradedQuantity()).isZero();
        assertThat(result.getOrder().getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(result.getOrder().getRemainingQuantity()).isEqualTo(5L);
    }

    @Test
    @DisplayName("가격이 맞는 상대 주문이 있으면 즉시 체결된다")
    void matchesImmediately() {
        long price = stock.getCurrentPrice();
        orderService.placeOrder(SELLER, sell(stock, price, 10L));

        OrderResultDto result = body(orderService.placeOrder(BUYER, buy(stock, price, 10L)));

        assertThat(result.getTradedQuantity()).isEqualTo(10L);
        assertThat(result.getTradedAmount()).isEqualTo(price * 10);
        assertThat(result.getOrder().getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    @DisplayName("가격 우선 — 싼 매도 호가부터 체결된다")
    void pricePriority() {
        long price = stock.getCurrentPrice();
        // 일부러 비싼 것을 먼저 걸어, 시간순이 아니라 가격순으로 잡히는지 본다.
        orderService.placeOrder(SELLER, sell(stock, price + 200, 10L));
        orderService.placeOrder(SELLER, sell(stock, price + 100, 10L));

        OrderResultDto result = body(orderService.placeOrder(BUYER, buy(stock, price + 200, 10L)));

        assertThat(result.getTradedQuantity()).isEqualTo(10L);
        assertThat(result.getTrades()).singleElement()
                .satisfies(t -> assertThat(t.getPrice()).isEqualTo(price + 100));
    }

    @Test
    @DisplayName("시간 우선 — 같은 가격이면 먼저 낸 주문이 먼저 체결된다")
    void timePriority() {
        long price = stock.getCurrentPrice();
        Long firstId = orderId(orderService.placeOrder(SELLER, sell(stock, price, 10L)));
        Long secondId = orderId(orderService.placeOrder(SELLER, sell(stock, price, 10L)));

        orderService.placeOrder(BUYER, buy(stock, price, 10L));

        assertThat(orderRepository.findById(firstId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.FILLED);
        assertThat(orderRepository.findById(secondId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.OPEN);
    }

    @Test
    @DisplayName("여러 호가에 걸쳐 부분 체결되고 남은 수량은 호가창에 등록된다")
    void partialFill_acrossLevels() {
        long price = stock.getCurrentPrice();
        orderService.placeOrder(SELLER, sell(stock, price, 6L));
        orderService.placeOrder(SELLER, sell(stock, price + 100, 4L));

        // 15주를 원하지만 시장에는 10주뿐이다.
        OrderResultDto result = body(orderService.placeOrder(BUYER, buy(stock, price + 100, 15L)));

        assertThat(result.getTradedQuantity()).isEqualTo(10L);
        assertThat(result.getTradedAmount()).isEqualTo(price * 6 + (price + 100) * 4);
        assertThat(result.getTrades()).hasSize(2);
        assertThat(result.getOrder().getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(result.getOrder().getRemainingQuantity()).isEqualTo(5L);
    }

    @Test
    @DisplayName("싸게 체결되면 차액을 돌려준다 (가격 개선)")
    void refundsPriceImprovement() {
        long price = stock.getCurrentPrice();
        orderService.placeOrder(SELLER, sell(stock, price, 10L));

        long before = balanceOf(BUYER);
        // 상대 호가보다 100원 비싸게 부르지만 실제로는 상대 호가에 체결된다.
        orderService.placeOrder(BUYER, buy(stock, price + 100, 10L));
        long after = balanceOf(BUYER);

        // 빠져나간 돈은 부른 값이 아니라 실제 체결가여야 한다.
        assertThat(before - after).isEqualTo(price * 10);
    }

    @Test
    @DisplayName("자기 주문과는 체결되지 않는다 (자전거래 방지)")
    void doesNotMatchOwnOrder() {
        long price = stock.getCurrentPrice();
        // 매도와 매수를 같은 계좌로 낸다. market01은 물량과 예수금을 모두 가지고 있다.
        orderService.placeOrder(SELLER, sell(stock, price, 10L));

        OrderResultDto result = body(orderService.placeOrder(SELLER, buy(stock, price, 10L)));

        assertThat(result.getTradedQuantity()).isZero();
        assertThat(result.getOrder().getStatus()).isEqualTo(OrderStatus.OPEN);
    }

    @Test
    @DisplayName("체결가가 종목의 현재가가 된다")
    void tradePriceBecomesCurrentPrice() {
        long price = stock.getCurrentPrice();
        orderService.placeOrder(SELLER, sell(stock, price + 300, 5L));
        orderService.placeOrder(BUYER, buy(stock, price + 300, 5L));

        assertThat(stockRepository.findById(stock.getId()).orElseThrow().getCurrentPrice())
                .isEqualTo(price + 300);
    }

    @Test
    @DisplayName("매수하면 보유 수량이 누적되고 평균 단가가 다시 계산된다")
    void accumulatesHolding() {
        long price = stock.getCurrentPrice();
        orderService.placeOrder(SELLER, sell(stock, price, 10L));
        orderService.placeOrder(BUYER, buy(stock, price, 10L));
        orderService.placeOrder(SELLER, sell(stock, price + 1000, 10L));
        orderService.placeOrder(BUYER, buy(stock, price + 1000, 10L));

        Holding holding = holdingRepository
                .findByAccountAndStock(accountRepository.findById(BUYER).orElseThrow(), stock)
                .orElseThrow();

        assertThat(holding.getQuantity()).isEqualTo(20L);
        // (price*10 + (price+1000)*10) / 20 = price + 500
        assertThat(holding.getAveragePrice()).isEqualTo(price + 500);
    }

    @Test
    @DisplayName("전량 매도하면 보유 목록에서 사라진다")
    void removesHolding_whenFullySold() {
        long price = stock.getCurrentPrice();
        orderService.placeOrder(SELLER, sell(stock, price, 10L));
        orderService.placeOrder(BUYER, buy(stock, price, 10L));

        Account buyer = accountRepository.findById(BUYER).orElseThrow();
        assertThat(holdingRepository.findByAccountAndStock(buyer, stock)).isPresent();

        // 산 만큼 전부 되판다. 0주짜리 보유 레코드가 남으면 자산 화면에 계속 보인다.
        orderService.placeOrder(BUYER, sell(stock, price, 10L));

        assertThat(holdingRepository.findByAccountAndStock(buyer, stock)).isEmpty();
    }

    @Test
    @DisplayName("예수금보다 큰 매수는 거절된다")
    void rejectsWhenInsufficientFunds() {
        assertThatThrownBy(() ->
                orderService.placeOrder(BUYER, buy(stock, stock.getCurrentPrice(), 1_000_000L)))
                .isInstanceOf(ResponseException.class)
                .hasMessageContaining("예수금");
    }

    @Test
    @DisplayName("보유 수량보다 큰 매도는 거절된다")
    void rejectsWhenInsufficientQuantity() {
        assertThatThrownBy(() ->
                orderService.placeOrder(BUYER, sell(stock, stock.getCurrentPrice(), 10L)))
                .isInstanceOf(ResponseException.class);
    }

    // ── helpers ─────────────────────────────────────────────

    private OrderRequest buy(Stock stock, long price, long quantity) {
        return request(stock, OrderSide.BUY, OrderType.LIMIT, price, quantity);
    }

    private OrderRequest sell(Stock stock, long price, long quantity) {
        return request(stock, OrderSide.SELL, OrderType.LIMIT, price, quantity);
    }

    private OrderRequest request(Stock stock, OrderSide side, OrderType type, Long price, long quantity) {
        OrderRequest request = new OrderRequest();
        request.setStockId(stock.getId());
        request.setSide(side);
        request.setType(type);
        request.setPrice(price);
        request.setQuantity(quantity);
        return request;
    }

    private OrderResultDto body(Response response) {
        return (OrderResultDto) response.getBody();
    }

    private Long orderId(Response response) {
        return body(response).getOrder().getOrderId();
    }

    private long balanceOf(String accountId) {
        return accountRepository.findById(accountId).map(Account::getBalance).orElseThrow();
    }

    @SuppressWarnings("unused")
    private List<Order> openOrders(Stock stock) {
        return orderRepository.findAll();
    }
}
