package com.sk.skala.shopapi.controller;

import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.domain.Product;
import com.sk.skala.shopapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "상품", description = "상품 등록·조회·수정·삭제 API")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/list")
    @Operation(summary = "전체 상품 목록 조회", description = "offset/count로 페이지 단위 조회합니다")
    public Response getAllProducts(
            @RequestParam(value = "offset", defaultValue = "0") Integer offset,
            @RequestParam(value = "count", defaultValue = "10") Integer count) {
        return productService.getAllProducts(offset, count);
    }

    @GetMapping("/{id}")
    @Operation(summary = "개별 상품 조회", description = "ID로 상품 상세를 조회합니다")
    public Response getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PostMapping
    @Operation(summary = "상품 등록", description = "상품명 중복은 허용하지 않습니다")
    public Response createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    @PutMapping
    @Operation(summary = "상품 수정", description = "본문에 id를 포함해야 합니다")
    public Response updateProduct(@RequestBody Product product) {
        return productService.updateProduct(product);
    }

    @DeleteMapping
    @Operation(summary = "상품 삭제", description = "주문 내역이 있는 상품은 삭제할 수 없습니다")
    public Response deleteProduct(@RequestBody Product product) {
        return productService.deleteProduct(product);
    }
}
