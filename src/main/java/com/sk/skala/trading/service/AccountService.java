package com.sk.skala.trading.service;

import com.sk.skala.trading.common.Error;
import com.sk.skala.trading.common.PagedList;
import com.sk.skala.trading.common.Response;
import com.sk.skala.trading.common.SessionHandler;
import com.sk.skala.trading.domain.Account;
import com.sk.skala.trading.dto.AccountSession;
import com.sk.skala.trading.dto.AccountSummaryDto;
import com.sk.skala.trading.dto.HoldingDto;
import com.sk.skala.trading.exception.ParameterException;
import com.sk.skala.trading.exception.ResponseException;
import com.sk.skala.trading.repository.AccountRepository;
import com.sk.skala.trading.repository.HoldingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final SessionHandler sessionHandler;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.account.initial-balance:10000000}")
    private Long initialBalance;

    @Transactional
    public Response createAccount(AccountSession request) {
        if (isEmpty(request.getAccountId()) || isEmpty(request.getPassword())) {
            throw new ParameterException("accountId", "password");
        }
        if (accountRepository.existsById(request.getAccountId())) {
            throw new ResponseException(Error.DATA_DUPLICATED,
                    "이미 존재하는 계좌 ID입니다: " + request.getAccountId());
        }

        // 비밀번호는 해시로 저장한다. DB가 유출돼도 원문을 알 수 없다.
        Account account = new Account(
                request.getAccountId(),
                passwordEncoder.encode(request.getPassword()),
                initialBalance);
        accountRepository.save(account);

        AccountSession result = new AccountSession();
        result.setAccountId(account.getAccountId());
        result.setBalance(account.getBalance());
        return Response.success(result);
    }

    public Response login(AccountSession request) {
        if (isEmpty(request.getAccountId()) || isEmpty(request.getPassword())) {
            throw new ParameterException("accountId", "password");
        }

        // 존재하지 않는 ID인지 비밀번호가 틀린 것인지 구분해 알려주지 않는다.
        // 구분해 주면 어떤 ID가 개설되어 있는지 알아낼 수 있다.
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResponseException(Error.NOT_AUTHENTICATED,
                        "계좌 ID 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new ResponseException(Error.NOT_AUTHENTICATED, "계좌 ID 또는 비밀번호가 올바르지 않습니다");
        }

        AccountSession session = new AccountSession();
        session.setAccountId(account.getAccountId());
        session.setBalance(account.getBalance());
        session.setAccessToken(sessionHandler.storeAccessToken(account.getAccountId()));
        return Response.success(session);
    }

    public Response logout() {
        sessionHandler.clear();
        return Response.success();
    }

    /** 계좌 종합 현황 — 예수금·보유종목·평가손익·총자산 */
    public Response getSummary(String accountId) {
        Account account = findAccount(accountId);
        List<HoldingDto> holdings = holdingRepository.findByAccountId(accountId).stream()
                .map(HoldingDto::from)
                .toList();
        return Response.success(AccountSummaryDto.of(account, holdings));
    }

    /** 로그인한 본인 계좌 현황 */
    public Response getMySummary() {
        return getSummary(sessionHandler.getCurrentAccountId());
    }

    public Response getAllAccounts(int offset, int count) {
        Page<Account> page = accountRepository.findAll(
                PageRequest.of(offset / Math.max(count, 1), count, Sort.by("accountId").ascending()));
        // 비밀번호가 섞이지 않도록 엔티티 대신 DTO로 변환해 내려준다.
        return Response.success(PagedList.of(page, offset, count, a -> {
            AccountSession s = new AccountSession();
            s.setAccountId(a.getAccountId());
            s.setBalance(a.getBalance());
            return s;
        }));
    }

    @Transactional
    public Response deleteAccount(String accountId) {
        Account account = findAccount(accountId);
        accountRepository.delete(account);
        return Response.success();
    }

    private Account findAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND,
                        "계좌를 찾을 수 없습니다: " + accountId));
    }

    private boolean isEmpty(String v) {
        return v == null || v.isBlank();
    }
}
