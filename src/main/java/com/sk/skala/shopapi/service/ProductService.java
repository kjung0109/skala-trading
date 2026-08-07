package com.sk.skala.shopapi.service;

import com.sk.skala.shopapi.common.Error;
import com.sk.skala.shopapi.common.PagedList;
import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.domain.Product;
import com.sk.skala.shopapi.exception.ParameterException;
import com.sk.skala.shopapi.exception.ResponseException;
import com.sk.skala.shopapi.repository.OrderItemRepository;
import com.sk.skala.shopapi.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    /** 전체 상품 목록 (페이지 단위) */
    public Response getAllProducts(int offset, int count) {
        Pageable pageable = PageRequest.of(offset / Math.max(count, 1), count, Sort.by("id").ascending());
        Page<Product> page = productRepository.findAll(pageable);
        return Response.success(PagedList.of(page, offset, count));
    }

    public Response getProductById(Long id) {
        Product product = findProduct(id);
        return Response.success(product);
    }

    @Transactional
    public Response createProduct(Product product) {
        validate(product);

        if (productRepository.existsByProductName(product.getProductName())) {
            throw new ResponseException(Error.DATA_DUPLICATED,
                    "이미 존재하는 상품입니다: " + product.getProductName());
        }

        // ID는 JPA가 채우도록 비운다. 클라이언트가 보낸 값을 그대로 쓰면 기존 데이터를 덮어쓸 수 있다.
        product.setId(null);
        return Response.success(productRepository.save(product));
    }

    @Transactional
    public Response updateProduct(Product product) {
        if (product.getId() == null) {
            throw new ParameterException("id");
        }
        validate(product);

        Product saved = findProduct(product.getId());

        // 이름을 실제로 바꾸는 경우에만 중복을 확인한다.
        // 자기 이름을 그대로 보낸 수정 요청이 중복으로 막히면 안 되기 때문이다.
        if (!saved.getProductName().equals(product.getProductName())
                && productRepository.existsByProductName(product.getProductName())) {
            throw new ResponseException(Error.DATA_DUPLICATED,
                    "이미 존재하는 상품입니다: " + product.getProductName());
        }

        saved.setProductName(product.getProductName());
        saved.setProductPrice(product.getProductPrice());
        // 영속 상태라 더티 체킹으로 UPDATE가 나간다.
        return Response.success(saved);
    }

    @Transactional
    public Response deleteProduct(Product product) {
        if (product.getId() == null) {
            throw new ParameterException("id");
        }
        Product saved = findProduct(product.getId());

        // 주문에 걸린 상품을 지우면 주문 이력이 깨진다. 먼저 막아 원인을 분명히 알린다.
        if (orderItemRepository.existsByProduct(saved)) {
            throw new ResponseException(Error.DATA_DUPLICATED,
                    "주문 내역이 있는 상품은 삭제할 수 없습니다: " + saved.getProductName());
        }

        productRepository.delete(saved);
        return Response.success();
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "상품을 찾을 수 없습니다: " + id));
    }

    private void validate(Product product) {
        boolean nameEmpty = product.getProductName() == null || product.getProductName().isBlank();
        boolean priceInvalid = product.getProductPrice() == null || product.getProductPrice() <= 0;

        if (nameEmpty && priceInvalid) {
            throw new ParameterException("productName", "productPrice");
        }
        if (nameEmpty) {
            throw new ParameterException("productName");
        }
        if (priceInvalid) {
            throw new ParameterException("productPrice");
        }
    }
}
