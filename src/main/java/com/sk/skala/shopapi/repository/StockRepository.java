package com.sk.skala.shopapi.repository;

import com.sk.skala.shopapi.domain.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * 매칭을 시작하기 전에 종목 행을 잠근다.
     *
     * 호가창 매칭은 "조회 → 판단 → 갱신"이 원자적이어야 한다.
     * 두 요청이 같은 종목의 호가창을 동시에 읽으면 같은 매도 주문을 각각 체결 대상으로
     * 잡아 잔량이 음수가 될 수 있다. 종목 단위로 직렬화해 이를 막는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.id = :id")
    Optional<Stock> findByIdForUpdate(Long id);
}
