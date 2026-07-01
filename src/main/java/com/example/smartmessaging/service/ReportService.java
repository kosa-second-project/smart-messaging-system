package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.ReportSearchRequest;
import com.example.smartmessaging.dto.response.ReportPageResponse;

public interface ReportService {

    ReportPageResponse getDeliveryReport(ReportSearchRequest request);

    ReportPageResponse getChannelReport(ReportSearchRequest request);

    ReportPageResponse getCostReport(ReportSearchRequest request);

    ReportPageResponse getCustomerReport(ReportSearchRequest request);

    ReportPageResponse getPerformanceReport(ReportSearchRequest request);
}
