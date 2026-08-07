package com.sk.skala.shopapi.common;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 페이지 단위 조회 결과.
 * 전체 건수와 현재 위치를 함께 내려줘야 클라이언트가 다음 페이지 유무를 판단할 수 있다.
 */
@Getter
public class PagedList<T> {

    private final long total;      // 전체 건수
    private final int offset;      // 조회 시작 위치
    private final int count;       // 요청한 개수
    private final boolean hasNext; // 다음 페이지 존재 여부
    private final List<T> list;

    private PagedList(long total, int offset, int count, boolean hasNext, List<T> list) {
        this.total = total;
        this.offset = offset;
        this.count = count;
        this.hasNext = hasNext;
        this.list = list;
    }

    public static <E> PagedList<E> of(Page<E> page, int offset, int count) {
        return new PagedList<>(page.getTotalElements(), offset, count, page.hasNext(), page.getContent());
    }

    /** 엔티티 페이지를 DTO 리스트로 바꿔 담을 때 사용한다. */
    public static <E, D> PagedList<D> of(Page<E> page, int offset, int count, Function<E, D> mapper) {
        return new PagedList<>(page.getTotalElements(), offset, count, page.hasNext(),
                page.getContent().stream().map(mapper).toList());
    }
}
