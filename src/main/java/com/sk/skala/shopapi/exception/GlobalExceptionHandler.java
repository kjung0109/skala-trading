package com.sk.skala.shopapi.exception;

import com.sk.skala.shopapi.common.Error;
import com.sk.skala.shopapi.common.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * 모든 예외를 공통 Response 형식으로 변환한다.
 *
 * ResponseEntityExceptionHandler를 상속하는 이유:
 * 이 부모가 Spring MVC 표준 예외(경로 없음·메서드 불일치·본문 파싱 실패 등)를 이미
 * 알맞은 상태 코드로 처리한다. 상속하지 않고 @ExceptionHandler(Exception.class)만 두면
 * 그 예외들까지 잡아 전부 500으로 만들어 버린다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResponseException.class)
    public ResponseEntity<Response> handleResponse(ResponseException e) {
        Error error = e.getError();
        log.warn("[{}] {}", error, e.getMessage());
        return ResponseEntity.status(error.getStatus())
                .body(Response.fail(error, e.getMessage()));
    }

    @ExceptionHandler(ParameterException.class)
    public ResponseEntity<Response> handleParameter(ParameterException e) {
        return ResponseEntity.status(Error.INVALID_PARAMETER.getStatus())
                .body(Response.fail(Error.INVALID_PARAMETER, e.getMessage(), e.getParameters()));
    }

    /**
     * 동시에 같은 데이터를 수정해 낙관적 락에 걸린 경우.
     * 서버 장애가 아니라 재시도하면 되는 상황이므로 409로 구분해 알린다.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Response> handleConcurrentUpdate(OptimisticLockingFailureException e) {
        log.warn("동시 수정 충돌: {}", e.getMessage());
        return ResponseEntity.status(Error.CONCURRENT_UPDATE.getStatus())
                .body(Response.fail(Error.CONCURRENT_UPDATE, Error.CONCURRENT_UPDATE.getDefaultMessage()));
    }

    /** @Valid 검증 실패. 어느 필드가 왜 거부됐는지 함께 내려준다. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> "%s: %s".formatted(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return ResponseEntity.status(Error.INVALID_PARAMETER.getStatus())
                .body(Response.fail(Error.INVALID_PARAMETER, "입력값 검증에 실패했습니다", details));
    }

    /** 부모가 처리하는 표준 예외들도 공통 Response 형식으로 감싼다. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {

        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        Error error = switch (status) {
            case NOT_FOUND -> Error.DATA_NOT_FOUND;
            case BAD_REQUEST, METHOD_NOT_ALLOWED, UNSUPPORTED_MEDIA_TYPE -> Error.INVALID_PARAMETER;
            default -> Error.INTERNAL_ERROR;
        };

        return new ResponseEntity<>(
                Response.fail(error, status.getReasonPhrase()), headers, statusCode);
    }

    /** 위에서 잡지 못한 예외. 원인은 로그로만 남기고 클라이언트에 노출하지 않는다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return ResponseEntity.status(Error.INTERNAL_ERROR.getStatus())
                .body(Response.fail(Error.INTERNAL_ERROR, Error.INTERNAL_ERROR.getDefaultMessage()));
    }
}
