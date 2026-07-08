package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.DashboardQueueStatusResponse;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse;
import com.example.smartmessaging.dto.response.StatCardResponse;
import com.example.smartmessaging.service.DashboardService;
import com.example.smartmessaging.service.queue.RabbitQueueStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {
    private final DashboardService dashboardService;
    private final RabbitQueueStatusService rabbitQueueStatusService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary(@ModelAttribute StatSearchRequest request) {
        request.validate();
        return dashboardService.getSummary(request);
    }

    @GetMapping("/realtime-cards")
    public List<StatCardResponse> getRealtimeCards() {
        return dashboardService.getRealtimeCards();
    }

    @GetMapping("/queue-status")
    public DashboardQueueStatusResponse getQueueStatus() {
        return rabbitQueueStatusService.getDashboardQueueStatus();
    }}
