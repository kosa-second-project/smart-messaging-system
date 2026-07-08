package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse;
import com.example.smartmessaging.dto.response.StatCardResponse;

import java.util.List;

public interface DashboardService {
    DashboardSummaryResponse getSummary(StatSearchRequest request);

    List<StatCardResponse> getRealtimeCards();
}
