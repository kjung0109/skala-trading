package com.sk.skala.trading.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 계좌 개설·로그인 요청과 응답에 함께 쓴다. 비밀번호는 응답에 실리지 않는다. */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountSession {

    @NotBlank(message = "계좌 ID는 필수입니다")
    private String accountId;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    private String accessToken;
    private Long balance;
}
