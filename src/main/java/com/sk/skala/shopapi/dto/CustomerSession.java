package com.sk.skala.shopapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 로그인 요청과 응답에 함께 쓰는 DTO. 비밀번호는 응답에 실리지 않는다. */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerSession {

    @NotBlank(message = "고객 ID는 필수입니다")
    private String customerId;

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "비밀번호는 필수입니다")
    private String customerPassword;

    private String accessToken;
    private Double customerPoint;
}
