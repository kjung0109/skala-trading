package com.sk.skala.shopapi.dto;

import com.sk.skala.shopapi.domain.Account;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 계좌 종합 현황. 예수금·평가금액·총자산·수익률을 한 번에 보여준다. */
@Getter
@Builder
public class AccountSummaryDto {

    private String accountId;
    private Long balance;          // 예수금
    private Long totalValuation;   // 보유 종목 평가금액
    private Long totalAssets;      // 총자산 = 예수금 + 평가금액
    private Long totalInvestment;  // 매입 원가
    private Long totalProfitLoss;  // 평가 손익
    private Double totalReturnRate;
    private List<HoldingDto> holdings;

    public static AccountSummaryDto of(Account account, List<HoldingDto> holdings) {
        long valuation = holdings.stream().mapToLong(HoldingDto::getValuation).sum();
        long investment = holdings.stream()
                .mapToLong(h -> h.getAveragePrice() * h.getQuantity()).sum();
        long profitLoss = valuation - investment;

        return AccountSummaryDto.builder()
                .accountId(account.getAccountId())
                .balance(account.getBalance())
                .totalValuation(valuation)
                .totalAssets(account.getBalance() + valuation)
                .totalInvestment(investment)
                .totalProfitLoss(profitLoss)
                .totalReturnRate(investment == 0 ? 0.0
                        : Math.round(profitLoss * 10000.0 / investment) / 100.0)
                .holdings(holdings)
                .build();
    }
}
