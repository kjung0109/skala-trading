package com.sk.skala.trading.order;

import com.sk.skala.trading.account.Account;
import com.sk.skala.trading.account.AccountRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 시장가 주문 검증.
 *
 * 시장가는 지정가와 두 가지가 다르다.
 *  - 가격을 부르지 않고 걸려 있는 호가를 위에서부터 훑는다.
 *  - 다 채우지 못한 잔량은 호가창에 남기지 않고 소멸(EXPIRED)시킨다.
 *
 * 잔량을 남기면 "가격 없는 주문"이 호가창에 뜨는데, 실제로 그렇게 만들었다가
 * 매수 호가에 Long.MAX_VALUE가 표시되는 문제를 겪어 EXPIRED 상태를 추가했다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MarketOrderTest {

    @Autowired private OrderService orderService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private StockRepository stockRepository;

    private static final String SELLER = "market01";
    private static final String BUYER = "trader01";

    private Stock stock;

    @BeforeEach
    void setUp() {
        stock = stockRepository.findAll().get(0);
    }

    @Test
    @DisplayName("시장가 매수는 싼 호가부터 훑어 체결된다")
    void marketBuy_walksTheBook() {
        long price = stock.getCurrentPrice();
        orderService.placeOrder(SELLER, sell(price + 100, 4L));
        orderService.placeOrder(SELLER, sell(price, 6L));

        OrderResultDto result = body(orderService.placeOrder(BUYER, marketBuy(10L)));

        assertThat(result.getTradedQuantity()).isEqualTo(10L);
        assertThat(result.getTradedAmount()).isEqualTo(price * 6 + (price + 100) * 4);
        assertThat(result.getOrder().getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    @DisplayName("실제 체결된 금액만 예수금에서 빠진다")
    void marketBuy_chargesOnlyWhatWasFilled() {
        long price = stock.getCurrentPrice();
        orderService.placeOrder(SELLER, sell(price, 10L));

        long before = balanceOf(BUYER);
        orderService.placeOrder(BUYER, marketBuy(10L));

        assertThat(before - balanceOf(BUYER)).isEqualTo(price * 10);
    }

    @Test
    @DisplayName("호가 물량이 모자라면 남은 수량은 소멸한다")
    void marketBuy_expiresRemainder() {
        long price = stock.getCurrentPrice();
        orderService.placeOrder(SELLER, sell(price, 3L));

        OrderResultDto result = body(orderService.placeOrder(BUYER, marketBuy(10L)));

        assertThat(result.getTradedQuantity()).isEqualTo(3L);
        assertThat(result.getOrder().getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(result.getOrder().getRemainingQuantity()).isEqualTo(7L);
    }

    @Test
    @DisplayName("소멸한 잔량은 호가창에 남지 않는다")
    void expiredRemainder_isNotInOrderBook() {
        orderService.placeOrder(SELLER, sell(stock.getCurrentPrice(), 3L));
        orderService.placeOrder(BUYER, marketBuy(10L));

        // 매수 잔량 7주가 호가로 남았다면 여기에 잡힌다.
        assertThat(orderService.getOrderBook(stock.getId())).isNotNull();
        assertThat(hasBidLevels()).isFalse();
    }

    @Test
    @DisplayName("걸린 호가가 하나도 없으면 주문 자체를 거절한다")
    void marketBuy_withoutLiquidity() {
        // 한 주도 못 사고 소멸시키느니, 왜 안 됐는지 알려주고 지정가를 권하는 편이 낫다.
        assertThatThrownBy(() -> orderService.placeOrder(BUYER, marketBuy(10L)))
                .isInstanceOf(ResponseException.class)
                .hasMessageContaining("체결 가능한 호가가 없습니다");
    }

    // ── helpers ─────────────────────────────────────────────

    private boolean hasBidLevels() {
        var book = (com.sk.skala.trading.order.dto.OrderBookDto)
                orderService.getOrderBook(stock.getId()).getBody();
        return !book.getBidLevels().isEmpty();
    }

    private OrderRequest sell(long price, long quantity) {
        OrderRequest request = base(OrderSide.SELL, quantity);
        request.setType(OrderType.LIMIT);
        request.setPrice(price);
        return request;
    }

    private OrderRequest marketBuy(long quantity) {
        OrderRequest request = base(OrderSide.BUY, quantity);
        request.setType(OrderType.MARKET);
        return request;
    }

    private OrderRequest base(OrderSide side, long quantity) {
        OrderRequest request = new OrderRequest();
        request.setStockId(stock.getId());
        request.setSide(side);
        request.setQuantity(quantity);
        return request;
    }

    private OrderResultDto body(Response response) {
        return (OrderResultDto) response.getBody();
    }

    private long balanceOf(String accountId) {
        return accountRepository.findById(accountId).map(Account::getBalance).orElseThrow();
    }
}
