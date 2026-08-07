package com.sk.skala.trading.order;

import com.sk.skala.trading.order.dto.OrderRequest;
import com.sk.skala.trading.stock.Stock;
import com.sk.skala.trading.stock.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시 주문 정합성 검증.
 *
 * 같은 매도 호가를 두 매수 주문이 동시에 노리면, 잠금이 없을 때 둘 다 "10주가 남아 있다"를
 * 읽고 각자 10주를 체결시켜 잔량이 음수가 된다. 없는 물량이 팔리는 것이다.
 * OrderService가 종목 단위 비관적 락(PESSIMISTIC_WRITE)으로 이를 막는지 확인한다.
 *
 * 이 테스트에는 @Transactional을 붙이지 않는다.
 * 스레드마다 별도 트랜잭션에서 실제로 커밋돼야 경쟁 상황이 재현되기 때문이다.
 * 대신 다른 테스트가 쓰지 않는 종목·계좌만 건드려 커밋된 데이터가 영향을 주지 않게 한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrentOrderTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private StockRepository stockRepository;

    private static final String SELLER = "market01";
    /** 다른 테스트가 쓰지 않는 계좌 두 개 */
    private static final String[] BUYERS = {"trader02", "trader03"};

    @Test
    @DisplayName("10주 호가에 10주 매수 둘이 동시에 들어와도 딱 10주만 체결된다")
    void doesNotOversell() throws Exception {
        List<Stock> stocks = stockRepository.findAll();
        // 다른 테스트는 첫 종목만 쓴다. 마지막 종목을 골라 서로 간섭하지 않게 한다.
        Stock stock = stocks.get(stocks.size() - 1);
        long price = stock.getCurrentPrice();

        orderService.placeOrder(SELLER, sell(stock, price, 10L));

        AtomicLong traded = new AtomicLong();
        CountDownLatch ready = new CountDownLatch(BUYERS.length);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(BUYERS.length);

        ExecutorService pool = Executors.newFixedThreadPool(BUYERS.length);
        for (String buyer : BUYERS) {
            pool.submit(() -> {
                try {
                    ready.countDown();
                    go.await();   // 두 스레드가 최대한 같은 순간에 출발하도록 맞춘다
                    var result = orderService.placeOrder(buyer, buy(stock, price, 10L));
                    traded.addAndGet(((com.sk.skala.trading.order.dto.OrderResultDto)
                            result.getBody()).getTradedQuantity());
                } catch (Exception e) {
                    // 잠금 경합으로 거절되는 것은 정합성이 지켜졌다는 뜻이므로 실패가 아니다.
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        go.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        // 팔린 물량은 걸어둔 10주를 넘을 수 없다.
        assertThat(traded.get()).isEqualTo(10L);

        // 매도 주문의 잔량이 음수로 내려가지 않았는지도 확인한다.
        assertThat(orderRepository.findAll())
                .filteredOn(o -> o.getStock().getId().equals(stock.getId()))
                .allSatisfy(o -> assertThat(o.getRemainingQuantity()).isNotNegative());
    }

    private OrderRequest buy(Stock stock, long price, long quantity) {
        return request(stock, OrderSide.BUY, price, quantity);
    }

    private OrderRequest sell(Stock stock, long price, long quantity) {
        return request(stock, OrderSide.SELL, price, quantity);
    }

    private OrderRequest request(Stock stock, OrderSide side, long price, long quantity) {
        OrderRequest request = new OrderRequest();
        request.setStockId(stock.getId());
        request.setSide(side);
        request.setType(OrderType.LIMIT);
        request.setPrice(price);
        request.setQuantity(quantity);
        return request;
    }
}
