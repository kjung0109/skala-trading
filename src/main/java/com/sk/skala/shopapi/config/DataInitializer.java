package com.sk.skala.shopapi.config;

import com.sk.skala.shopapi.domain.Account;
import com.sk.skala.shopapi.domain.Holding;
import com.sk.skala.shopapi.repository.AccountRepository;
import com.sk.skala.shopapi.repository.HoldingRepository;
import com.sk.skala.shopapi.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * 데모용 초기 계좌를 만든다.
 *
 * data.sql에 BCrypt 해시를 직접 적어 넣지 않는 이유:
 * 해시 문자열은 사람이 검증할 수 없어 잘못된 값을 넣어도 로그인 실패로만 드러난다.
 * 인코더를 통해 만들면 비밀번호 원문이 코드에 남고 항상 맞는 해시가 생성된다.
 * (종목 데이터처럼 해시가 없는 것은 data.sql에 둔다)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private static final String DEMO_PASSWORD = "pw1234";

    @Bean
    public ApplicationRunner initData(AccountRepository accountRepository,
                                      StockRepository stockRepository,
                                      HoldingRepository holdingRepository,
                                      PasswordEncoder encoder) {
        return args -> {
            if (accountRepository.count() > 0) {
                return;
            }

            String encoded = encoder.encode(DEMO_PASSWORD);
            Account trader01 = new Account("trader01", encoded, 10_000_000L);
            Account trader02 = new Account("trader02", encoded, 20_000_000L);
            Account trader03 = new Account("trader03", encoded, 50_000_000L);
            accountRepository.saveAll(List.of(trader01, trader02, trader03));

            // 매도 주문이 나오려면 누군가는 이미 주식을 갖고 있어야 한다.
            // 시장에 유동성을 공급하는 계좌로 trader03에 초기 보유분을 준다.
            stockRepository.findAll().forEach(stock ->
                    holdingRepository.save(new Holding(trader03, stock, 500L, stock.getPreviousPrice())));

            log.info("데모 데이터 생성 완료 - 계좌 3개(비밀번호 {}), trader03에 종목별 500주 보유", DEMO_PASSWORD);
        };
    }
}
