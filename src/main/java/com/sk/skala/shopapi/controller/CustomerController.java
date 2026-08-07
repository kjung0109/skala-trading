package com.sk.skala.shopapi.controller;

import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.domain.Customer;
import com.sk.skala.shopapi.dto.CustomerSession;
import com.sk.skala.shopapi.dto.OrderRequest;
import com.sk.skala.shopapi.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "고객", description = "고객 관리 및 주문 API. 주문·취소는 로그인이 필요합니다")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/list")
    @Operation(summary = "전체 고객 목록 조회", description = "offset/count로 페이지 단위 조회합니다")
    public Response getAllCustomers(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "count", defaultValue = "10") int count) {
        return customerService.getAllCustomers(offset, count);
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "고객 상세 조회", description = "고객 정보와 주문한 상품 목록을 함께 반환합니다")
    public Response getCustomerById(@PathVariable String customerId) {
        return customerService.getCustomerById(customerId);
    }

    @PostMapping
    @Operation(summary = "고객 등록", description = "가입 시 초기 포인트가 지급됩니다. 비밀번호는 해시로 저장됩니다")
    public Response createCustomer(@Valid @RequestBody CustomerSession request) {
        return customerService.createCustomer(request);
    }

    @PostMapping("/login")
    @Operation(summary = "고객 로그인", description = "성공 시 JWT를 발급하고 쿠키(bff-access)로도 내려줍니다")
    public Response loginCustomer(@Valid @RequestBody CustomerSession request) {
        return customerService.loginCustomer(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "발급된 쿠키를 만료시킵니다")
    public Response logout() {
        return customerService.logout();
    }

    @PutMapping
    @Operation(summary = "고객 정보 수정", description = "포인트를 변경합니다")
    public Response updateCustomer(@RequestBody Customer customer) {
        return customerService.updateCustomer(customer);
    }

    @DeleteMapping
    @Operation(summary = "고객 삭제", description = "주문 내역도 함께 삭제됩니다")
    public Response deleteCustomer(@RequestBody Customer customer) {
        return customerService.deleteCustomer(customer);
    }

    @PostMapping("/order")
    @Operation(summary = "상품 주문", description = "포인트가 차감됩니다. 같은 상품을 다시 주문하면 수량이 누적됩니다")
    public Response placeOrder(@Valid @RequestBody OrderRequest order) {
        return customerService.placeOrder(order);
    }

    @PostMapping("/cancel")
    @Operation(summary = "주문 취소", description = "수량을 줄이고 결제 금액만큼 포인트를 환급합니다")
    public Response cancelOrder(@Valid @RequestBody OrderRequest order) {
        return customerService.cancelOrder(order);
    }
}
