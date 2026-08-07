package com.sk.skala.shopapi.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.sk.skala.shopapi.exception.ResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 발급과 검증을 담당한다.
 *
 * 토큰은 두 경로로 받는다.
 * - Authorization: Bearer ...  → Swagger·Postman에서 쓰기 편하다
 * - 쿠키 bff-access            → 브라우저 화면에서 자동 전송된다
 * 어느 쪽으로 와도 동작하게 해 두 환경에서 같은 API를 그대로 쓴다.
 */
@Slf4j
@Component
public class SessionHandler {

    public static final String COOKIE_NAME = "bff-access";
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final SecretKey key;
    private final long validityMillis;

    public SessionHandler(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.validity-seconds:3600}") long validitySeconds) {

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityMillis = validitySeconds * 1000;
    }

    /** 로그인 성공 시 토큰을 만들어 쿠키로도 내려준다. */
    public String storeAccessToken(String customerId) {
        Date now = new Date();
        String token = Jwts.builder()
                .setSubject(customerId)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validityMillis))
                .signWith(key)
                .compact();

        HttpServletResponse response = currentResponse();
        if (response != null) {
            Cookie cookie = new Cookie(COOKIE_NAME, token);
            cookie.setHttpOnly(true);   // 자바스크립트로 토큰을 읽지 못하게 한다
            cookie.setPath("/");
            cookie.setMaxAge((int) (validityMillis / 1000));
            response.addCookie(cookie);
        }
        return token;
    }

    /** 현재 요청의 로그인 고객 ID. 없거나 유효하지 않으면 예외. */
    public String getCurrentCustomerId() {
        String token = extractToken();
        if (token == null) {
            throw new ResponseException(Error.NOT_AUTHENTICATED, "로그인이 필요합니다");
        }
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();

        } catch (JwtException e) {
            // 위조·만료 모두 여기로 온다. 원인을 자세히 알려주면 공격에 도움이 되므로 뭉뚱그린다.
            log.warn("유효하지 않은 토큰: {}", e.getMessage());
            throw new ResponseException(Error.NOT_AUTHENTICATED, "인증 정보가 유효하지 않습니다");
        }
    }

    public void clear() {
        HttpServletResponse response = currentResponse();
        if (response != null) {
            Cookie cookie = new Cookie(COOKIE_NAME, "");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
    }

    private String extractToken() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private HttpServletResponse currentResponse() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getResponse();
    }
}
