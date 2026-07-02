package com.example.smartmessaging.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void JSONP_Accept_헤더는_JSON_요청으로_판단하지_않는다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.ACCEPT, "application/jsonp");

        Object result = exceptionHandler.handleBusinessException(
                new BusinessException(ErrorCode.SEND_HISTORY_NOT_FOUND),
                request,
                new ExtendedModelMap()
        );

        assertThat(result).isEqualTo("error/500");
    }

    @Test
    void 파라미터가_포함된_JSON_Accept_헤더는_JSON_요청으로_판단한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.ACCEPT, "application/json; charset=UTF-8");

        Object result = exceptionHandler.handleBusinessException(
                new BusinessException(ErrorCode.SEND_HISTORY_NOT_FOUND),
                request,
                new ExtendedModelMap()
        );

        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
