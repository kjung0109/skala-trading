package com.sk.skala.trading.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 주문 감사 로그.
 *
 * 거래가 실패해 롤백되더라도 "무엇을 시도했다가 왜 거절됐는지"는 남아야 한다.
 * 그래서 이 기록은 주문 트랜잭션과 분리된 트랜잭션에서 저장한다.
 * (OrderAuditService 참고)
 */
@Entity
@Getter
@Builder
@Table(name = "order_audit_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@lombok.AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderAuditLog {

    public enum Action {
        PLACE, CANCEL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Action action;

    /** 무엇을 요청했는지. "005930 BUY LIMIT 230,000 x 10" 같은 형태 */
    @Column(length = 200)
    private String detail;

    @Column(nullable = false)
    private boolean success;

    /** 성공하면 체결 요약, 실패하면 거절 사유 */
    @Column(length = 500)
    private String message;

    /** 처리에 걸린 시간(ms). 매칭 엔진이 느려지는지 추적한다. */
    private Long elapsedMs;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
