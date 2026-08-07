package com.sk.skala.trading.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAuditLogRepository extends JpaRepository<OrderAuditLog, Long> {

    Page<OrderAuditLog> findByAccountIdOrderByIdDesc(String accountId, Pageable pageable);
}
