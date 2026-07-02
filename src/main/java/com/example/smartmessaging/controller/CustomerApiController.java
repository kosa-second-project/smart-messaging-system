package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.CustomerSearchDTO;
import com.example.smartmessaging.dto.response.*;
import com.example.smartmessaging.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerApiController {

    private final CustomerService customerService;

    /**
     * 필터링 및 페이징이 포함된 고객 목록을 조회합니다.
     */
    @GetMapping
    public ResponseEntity<PageResponse<CustomerResponseDTO>> getCustomerList(CustomerSearchDTO searchDTO) {
        log.info("[CustomerApiController] 고객 목록 조회 API 호출 - Parameter: {}", searchDTO);
        PageResponse<CustomerResponseDTO> response = customerService.getCustomerList(searchDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * 고객 유형별 집계 통계 수치를 조회합니다. (상단 카드 데이터용)
     */
    @GetMapping("/stats")
    public ResponseEntity<CustomerStatResponseDTO> getCustomerStats() {
        log.info("[CustomerApiController] 고객 통계 조회 API 호출");
        CustomerStatResponseDTO response = customerService.getCustomerStats();
        return ResponseEntity.ok(response);
    }


    /**
     * 특정 고객의 과거 메시지 수신 이력 목록을 조회합니다.
     */
    @GetMapping("/{customerId}/history")
    public ResponseEntity<List<CustomerReceiveHistoryResponseDTO>> getCustomerReceiveHistory(@PathVariable("customerId") Long customerId) {
        log.info("[CustomerApiController] 고객 수신 이력 API 호출 - CustomerID: {}", customerId);
        List<CustomerReceiveHistoryResponseDTO> response = customerService.getCustomerReceiveHistory(customerId);
        return ResponseEntity.ok(response);
    }

    /**
     * 080 수신 거부자 목록을 조회합니다.
     */
    @GetMapping("/rejects")
    public ResponseEntity<PageResponse<RejectCustomerResponseDTO>> getRejectCustomerList(CustomerSearchDTO searchDTO) {
        log.info("[CustomerApiController] 080 수신거부자 목록 API 호출 - Parameter: {}", searchDTO);
        PageResponse<RejectCustomerResponseDTO> response = customerService.getRejectCustomerList(searchDTO);
        return ResponseEntity.ok(response);
    }
}
