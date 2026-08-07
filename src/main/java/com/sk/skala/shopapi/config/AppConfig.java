package com.sk.skala.shopapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

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
                .title("스칼라 온라인 쇼핑몰 API")
                .description("""
                        상품·고객·주문을 관리하는 REST API (백엔드 최종 실습과제)

                        [주문·취소는 로그인이 필요합니다]
                        POST /api/customers/login 으로 로그인하면 쿠키(bff-access)가 발급되어
                        이후 요청에 자동으로 실립니다. Swagger에서도 그대로 동작합니다.
                        """)
                .version("v1.0.0"));
    }
}
