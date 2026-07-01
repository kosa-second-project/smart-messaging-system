package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.ReportSearchRequest;
import com.example.smartmessaging.dto.response.ReportPageResponse;
import com.example.smartmessaging.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportApiController {

    private final ReportService reportService;

    @GetMapping("/delivery")
    public ReportPageResponse getDeliveryReport(@ModelAttribute ReportSearchRequest request) {
        return reportService.getDeliveryReport(request);
    }

    @GetMapping("/channel")
    public ReportPageResponse getChannelReport(@ModelAttribute ReportSearchRequest request) {
        return reportService.getChannelReport(request);
    }

    @GetMapping("/cost")
    public ReportPageResponse getCostReport(@ModelAttribute ReportSearchRequest request) {
        return reportService.getCostReport(request);
    }

    @GetMapping("/customer")
    public ReportPageResponse getCustomerReport(@ModelAttribute ReportSearchRequest request) {
        return reportService.getCustomerReport(request);
    }

    @GetMapping("/performance")
    public ReportPageResponse getPerformanceReport(@ModelAttribute ReportSearchRequest request) {
        return reportService.getPerformanceReport(request);
    }
}
