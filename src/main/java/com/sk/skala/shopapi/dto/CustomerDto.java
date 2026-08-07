package com.sk.skala.shopapi.dto;

import com.sk.skala.shopapi.domain.Customer;
import lombok.Builder;
import lombok.Getter;

/** 고객 조회 응답. 비밀번호를 애초에 담지 않는다. */
@Getter
@Builder
public class CustomerDto {

    private String customerId;
    private Double customerPoint;

    public static CustomerDto from(Customer customer) {
        return CustomerDto.builder()
                .customerId(customer.getCustomerId())
                .customerPoint(customer.getCustomerPoint())
                .build();
    }
}
