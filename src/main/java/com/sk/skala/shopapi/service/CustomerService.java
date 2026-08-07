package com.sk.skala.shopapi.service;

import com.sk.skala.shopapi.common.Error;
import com.sk.skala.shopapi.common.PagedList;
import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.common.SessionHandler;
import com.sk.skala.shopapi.domain.Customer;
import com.sk.skala.shopapi.domain.OrderItem;
import com.sk.skala.shopapi.domain.Product;
import com.sk.skala.shopapi.dto.*;
import com.sk.skala.shopapi.exception.ParameterException;
import com.sk.skala.shopapi.exception.ResponseException;
import com.sk.skala.shopapi.repository.CustomerRepository;
import com.sk.skala.shopapi.repository.OrderItemRepository;
import com.sk.skala.shopapi.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final SessionHandler sessionHandler;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.customer.initial-point:1000000}")
    private Double initialPoint;

    // ------------------------------------------------------------------
    // 조회
    // ------------------------------------------------------------------

    public Response getAllCustomers(int offset, int count) {
        Pageable pageable = PageRequest.of(offset / Math.max(count, 1), count,
                Sort.by("customerId").ascending());
        Page<Customer> page = customerRepository.findAll(pageable);
        // 비밀번호가 섞이지 않도록 엔티티가 아닌 DTO로 변환해 내려준다.
        return Response.success(PagedList.of(page, offset, count, CustomerDto::from));
    }

    /** 고객 정보 + 주문한 상품 목록 */
    public Response getCustomerById(String customerId) {
        Customer customer = findCustomer(customerId);

        var products = orderItemRepository.findByCustomerCustomerId(customerId).stream()
                .map(OrderItemDto::from)
                .toList();

        return Response.success(OrderListDto.builder()
                .customerId(customer.getCustomerId())
                .customerPoint(customer.getCustomerPoint())
                .products(products)
                .build());
    }

    // ------------------------------------------------------------------
    // 등록 · 로그인 · 수정 · 삭제
    // ------------------------------------------------------------------

    @Transactional
    public Response createCustomer(CustomerSession request) {
        if (isEmpty(request.getCustomerId()) || isEmpty(request.getCustomerPassword())) {
            throw new ParameterException("customerId", "customerPassword");
        }
        if (customerRepository.existsById(request.getCustomerId())) {
            throw new ResponseException(Error.DATA_DUPLICATED,
                    "이미 존재하는 고객 ID입니다: " + request.getCustomerId());
        }

        // 비밀번호는 해시로 저장한다. DB가 유출돼도 원문을 알 수 없다.
        Customer customer = new Customer(
                request.getCustomerId(),
                passwordEncoder.encode(request.getCustomerPassword()),
                initialPoint);

        customerRepository.save(customer);
        return Response.success(CustomerDto.from(customer));
    }

    @Transactional
    public Response loginCustomer(CustomerSession request) {
        if (isEmpty(request.getCustomerId()) || isEmpty(request.getCustomerPassword())) {
            throw new ParameterException("customerId", "customerPassword");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                // 존재하지 않는 ID인지 비밀번호가 틀린 것인지 구분해 알려주지 않는다.
                // 구분해 주면 어떤 ID가 가입되어 있는지 알아낼 수 있다.
                .orElseThrow(() -> new ResponseException(Error.NOT_AUTHENTICATED,
                        "아이디 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(request.getCustomerPassword(), customer.getCustomerPassword())) {
            throw new ResponseException(Error.NOT_AUTHENTICATED, "아이디 또는 비밀번호가 올바르지 않습니다");
        }

        String token = sessionHandler.storeAccessToken(customer.getCustomerId());

        CustomerSession session = new CustomerSession();
        session.setCustomerId(customer.getCustomerId());
        session.setCustomerPoint(customer.getCustomerPoint());
        session.setAccessToken(token);
        return Response.success(session);
    }

    public Response logout() {
        sessionHandler.clear();
        return Response.success();
    }

    @Transactional
    public Response updateCustomer(Customer request) {
        if (isEmpty(request.getCustomerId())) {
            throw new ParameterException("customerId");
        }
        if (request.getCustomerPoint() == null || request.getCustomerPoint() < 0) {
            throw new ParameterException("customerPoint");
        }

        Customer customer = findCustomer(request.getCustomerId());
        customer.setCustomerPoint(request.getCustomerPoint());
        return Response.success(CustomerDto.from(customer));
    }

    @Transactional
    public Response deleteCustomer(Customer request) {
        if (isEmpty(request.getCustomerId())) {
            throw new ParameterException("customerId");
        }
        Customer customer = findCustomer(request.getCustomerId());

        // 주문 내역을 먼저 정리해야 외래키 제약에 걸리지 않는다.
        orderItemRepository.deleteAll(orderItemRepository.findByCustomerCustomerId(customer.getCustomerId()));
        customerRepository.delete(customer);
        return Response.success();
    }

    // ------------------------------------------------------------------
    // 주문 · 취소
    // ------------------------------------------------------------------

    /**
     * 상품 주문.
     * 포인트 차감과 주문 반영이 한 단위로 묶여야 하므로 트랜잭션으로 처리한다.
     * 중간에 실패하면 차감된 포인트도 함께 되돌아간다.
     */
    @Transactional
    public Response placeOrder(OrderRequest order) {
        Customer customer = findCustomer(sessionHandler.getCurrentCustomerId());
        Product product = findProduct(order.getProductId());

        double amount = product.getProductPrice() * order.getQuantity();
        customer.pay(amount);   // 잔액 부족이면 여기서 예외

        orderItemRepository.findByCustomerAndProduct(customer, product)
                .ifPresentOrElse(
                        item -> item.addQuantity(order.getQuantity()),
                        () -> orderItemRepository.save(
                                new OrderItem(customer, product, order.getQuantity())));

        return Response.success(currentOrderList(customer));
    }

    /** 주문 취소. 수량을 줄이고 결제 금액만큼 포인트를 돌려준다. */
    @Transactional
    public Response cancelOrder(OrderRequest order) {
        Customer customer = findCustomer(sessionHandler.getCurrentCustomerId());
        Product product = findProduct(order.getProductId());

        OrderItem item = orderItemRepository.findByCustomerAndProduct(customer, product)
                .orElseThrow(() -> new ResponseException(Error.INSUFFICIENT_QUANTITY,
                        "주문하지 않은 상품입니다: " + product.getProductName()));

        item.reduceQuantity(order.getQuantity());   // 수량 부족이면 여기서 예외
        customer.refund(product.getProductPrice() * order.getQuantity());

        if (item.isEmpty()) {
            orderItemRepository.delete(item);
        }
        return Response.success(currentOrderList(customer));
    }

    // ------------------------------------------------------------------

    private OrderListDto currentOrderList(Customer customer) {
        // 주문 직후 상태를 그대로 돌려줘 클라이언트가 다시 조회하지 않아도 되게 한다.
        orderItemRepository.flush();
        var products = orderItemRepository.findByCustomerCustomerId(customer.getCustomerId()).stream()
                .map(OrderItemDto::from)
                .toList();

        return OrderListDto.builder()
                .customerId(customer.getCustomerId())
                .customerPoint(customer.getCustomerPoint())
                .products(products)
                .build();
    }

    private Customer findCustomer(String customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND,
                        "고객을 찾을 수 없습니다: " + customerId));
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND,
                        "상품을 찾을 수 없습니다: " + productId));
    }

    private boolean isEmpty(String value) {
        return value == null || value.isBlank();
    }
}
