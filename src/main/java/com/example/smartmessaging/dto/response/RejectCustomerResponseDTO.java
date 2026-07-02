package com.example.smartmessaging.dto.response;

import lombok.Data;

@Data
public class RejectCustomerResponseDTO {
    
    private Long customerId;
    private String name;
    private String phone;
    private String rejectedAt;      // 거부 일시 (yyyy-MM-dd HH:mm)
}
