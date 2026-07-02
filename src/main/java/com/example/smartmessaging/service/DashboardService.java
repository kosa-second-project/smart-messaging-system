package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse;

public interface DashboardService {
    DashboardSummaryResponse getSummary(StatSearchRequest request);
}
