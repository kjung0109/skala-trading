package com.sk.skala.trading.config;

import com.sk.skala.trading.account.Account;
import com.sk.skala.trading.account.Holding;
import com.sk.skala.trading.order.Order;
import com.sk.skala.trading.order.OrderSide;
import com.sk.skala.trading.order.OrderType;
import com.sk.skala.trading.stock.Stock;
import com.sk.skala.trading.account.AccountRepository;
import com.sk.skala.trading.account.HoldingRepository;
import com.sk.skala.trading.order.OrderRepository;
import com.sk.skala.trading.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * 데모용 초기 데이터를 만든다.
 *
 * data.sql에 BCrypt 해시를 직접 적어 넣지 않는 이유:
 * 해시 문자열은 사람이 검증할 수 없어 잘못된 값을 넣어도 로그인 실패로만 드러난다.
 * 인코더를 통해 만들면 비밀번호 원문이 코드에 남고 항상 맞는 해시가 생성된다.
 * (종목처럼 해시가 없는 데이터는 data.sql에 둔다)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private static final String DEMO_PASSWORD = "pw1234";

    /** 유동성 공급 계좌. 앱이 뜰 때 호가창을 채워 첫 주문부터 체결되게 한다. */
    private static final String MARKET_MAKER = "market01";

    /**
     * 호가창 초기 시딩 여부.
     * 테스트에서는 호가를 직접 만들어 검증해야 하므로 false로 끈다.
     */
    @Value("${app.demo.seed-order-book:true}")
    private boolean seedOrderBook;

    @Bean
    public ApplicationRunner initData(AccountRepository accountRepository,
                                      StockRepository stockRepository,
                                      HoldingRepository holdingRepository,
                                      OrderRepository orderRepository,
                                      PasswordEncoder encoder) {
        return args -> {
            if (accountRepository.count() > 0) {
                return;
            }

            String encoded = encoder.encode(DEMO_PASSWORD);

            // 고가 종목(SK하이닉스 141만원 등)도 여러 주 거래할 수 있도록 예수금을 넉넉히 둔다.
            Account trader01 = new Account("trader01", encoded, 100_000_000L);
            Account trader02 = new Account("trader02", encoded, 300_000_000L);
            Account trader03 = new Account("trader03", encoded, 500_000_000L);
            // 유동성 공급 계좌는 종일 양쪽 호가를 대므로 자금이 넉넉해야 한다.
            // 모자라면 한쪽 호가부터 조용히 사라진다.
            Account marketMaker = new Account(MARKET_MAKER, encoded, 1_000_000_000_000L);

            // 자동 매매 봇 계좌.
            // 같은 계좌끼리는 체결되지 않으므로(자전거래 방지) 둘로 나눠 서로 상대가 되게 한다.
            Account bot01 = new Account("bot01", encoded, 1_000_000_000_000L);
            Account bot02 = new Account("bot02", encoded, 1_000_000_000_000L);

            accountRepository.saveAll(List.of(trader01, trader02, trader03, marketMaker, bot01, bot02));

            List<Stock> stocks = stockRepository.findAll();

            // 매도 주문을 내려면 주식을 갖고 있어야 한다. 유동성 공급 계좌에 물량을 준다.
            // 매도 주문을 내려면 주식을 갖고 있어야 한다. 공급 계좌와 봇에 물량을 준다.
            stocks.forEach(stock -> {
                holdingRepository.save(new Holding(marketMaker, stock, 1_000_000L, stock.getPreviousPrice()));
                holdingRepository.save(new Holding(bot01, stock, 500_000L, stock.getPreviousPrice()));
                holdingRepository.save(new Holding(bot02, stock, 500_000L, stock.getPreviousPrice()));
            });
            holdingRepository.flush();

            // 호가창을 미리 채운다.
            // 이게 없으면 첫 주문은 상대가 없어 체결되지 않고 호가창에 등록만 된다.
            if (seedOrderBook) {
                stocks.forEach(stock -> seedOrderBook(orderRepository, marketMaker, stock));
            }

            log.info("데모 데이터 생성 완료 - 계좌 {}개(비밀번호 {}), 종목 {}개 호가창 초기화, 봇 계좌 2개",
                    6, DEMO_PASSWORD, stocks.size());
        };
    }

    /**
     * 종목별 호가창 초기화.
     * 현재가를 기준으로 위아래 3단계씩 매도·매수 호가를 걸어둔다.
     * 호가 단위는 가격대마다 다르므로 현재가의 0.1% 정도를 한 칸으로 잡는다.
     */
    private void seedOrderBook(OrderRepository orderRepository, Account maker, Stock stock) {
        long price = stock.getCurrentPrice();
        long tick = Math.max(price / 1000, 1);

        for (int i = 1; i <= 3; i++) {
            long askPrice = price + tick * i;
            long bidPrice = price - tick * i;
            long quantity = 30L * i;   // 멀어질수록 물량이 많아지는 실제 호가창 모양

            orderRepository.save(new Order(maker, stock, OrderSide.SELL, OrderType.LIMIT, askPrice, quantity));
            if (bidPrice > 0) {
                orderRepository.save(new Order(maker, stock, OrderSide.BUY, OrderType.LIMIT, bidPrice, quantity));
            }
        }
    }
}
