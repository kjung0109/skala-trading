package com.sk.skala.trading.stock;

import com.sk.skala.trading.common.Error;
import com.sk.skala.trading.common.PagedList;
import com.sk.skala.trading.common.Response;
import com.sk.skala.trading.stock.Stock;
import com.sk.skala.trading.stock.dto.StockDto;
import com.sk.skala.trading.exception.ParameterException;
import com.sk.skala.trading.exception.ResponseException;
import com.sk.skala.trading.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;

    public Response getAllStocks(int offset, int count) {
        Page<Stock> page = stockRepository.findAll(
                PageRequest.of(offset / Math.max(count, 1), count, Sort.by("id").ascending()));
        return Response.success(PagedList.of(page, offset, count, StockDto::from));
    }

    public Response getStockById(Long id) {
        return Response.success(StockDto.from(findStock(id)));
    }

    public Response getStockByCode(String code) {
        Stock stock = stockRepository.findByCode(code)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "종목을 찾을 수 없습니다: " + code));
        return Response.success(StockDto.from(stock));
    }

    @Transactional
    public Response createStock(Stock stock) {
        validate(stock);
        if (stockRepository.existsByCode(stock.getCode())) {
            throw new ResponseException(Error.DATA_DUPLICATED, "이미 존재하는 종목 코드입니다: " + stock.getCode());
        }
        stock.setId(null);   // ID는 JPA가 채운다
        return Response.success(StockDto.from(stockRepository.save(stock)));
    }

    @Transactional
    public Response updateStock(Long id, Stock request) {
        validate(request);
        Stock stock = findStock(id);

        // 코드를 실제로 바꾸는 경우에만 중복을 확인한다.
        // 자기 코드를 그대로 보낸 수정 요청이 중복으로 막히면 안 되기 때문이다.
        if (!stock.getCode().equals(request.getCode()) && stockRepository.existsByCode(request.getCode())) {
            throw new ResponseException(Error.DATA_DUPLICATED, "이미 존재하는 종목 코드입니다: " + request.getCode());
        }

        stock.setCode(request.getCode());
        stock.setName(request.getName());
        stock.setCurrentPrice(request.getCurrentPrice());
        stock.setPreviousPrice(request.getPreviousPrice());
        return Response.success(StockDto.from(stock));
    }

    @Transactional
    public Response deleteStock(Long id) {
        stockRepository.delete(findStock(id));
        return Response.success();
    }

    private Stock findStock(Long id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "종목을 찾을 수 없습니다: " + id));
    }

    private void validate(Stock stock) {
        if (stock.getCode() == null || stock.getCode().isBlank()) {
            throw new ParameterException("code");
        }
        if (stock.getName() == null || stock.getName().isBlank()) {
            throw new ParameterException("name");
        }
        if (stock.getCurrentPrice() == null || stock.getCurrentPrice() <= 0) {
            throw new ParameterException("currentPrice");
        }
        if (stock.getPreviousPrice() == null || stock.getPreviousPrice() <= 0) {
            stock.setPreviousPrice(stock.getCurrentPrice());
        }
    }
}
