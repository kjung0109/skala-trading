package com.sk.skala.trading.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    /** Swagger Authorize 에 표시할 인증 방식 이름. 컨트롤러의 @SecurityRequirement 와 맞춰야 한다. */
    public static final String BEARER_AUTH = "bearerAuth";

    /**
     * 비밀번호 해시.
     * spring-security-crypto만 의존하므로 보안 필터체인이 끼어들지 않는다.
     * (starter-security를 넣으면 모든 엔드포인트에 로그인 화면이 걸린다)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OpenAPI shopOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("SKALA 증권 거래 시스템 API")
                .description("""
                        호가·체결 기반 주식 거래 REST API (백엔드 최종 실습과제)

                        외부 증권 API를 사용하지 않습니다. 주문 접수 · 호가 관리 · 매칭 ·
                        체결 · 정산을 직접 구현했으며, 화면의 현재가는 이 시스템 안에서
                        체결된 가격입니다.

                        [매칭 규칙]
                        가격 우선 → 시간 우선. 먼저 호가창에 있던 주문의 가격으로 체결하며,
                        나중에 온 주문이 유리한 가격을 냈다면 차액을 돌려줍니다.

                        [주문 유형]
                        LIMIT(지정가)  : 조건이 맞지 않으면 호가창에서 대기
                        MARKET(시장가) : 현재 호가로 즉시 체결, 못 채운 잔량은 소멸

                        데모 계좌: trader01 ~ trader03 / 비밀번호 pw1234
                        """)
                        .version("v1.0.0"))
                // 인증 방식을 문서에 선언해야 Swagger 화면에 Authorize 버튼이 생긴다.
                // 선언만으로는 어떤 API에 필요한지 알 수 없으므로,
                // 실제 표시는 각 컨트롤러의 @SecurityRequirement 가 결정한다.
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인 응답의 accessToken 값을 그대로 붙여넣습니다.")));
    }
}
