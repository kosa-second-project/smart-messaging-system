package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.StatPageResponse;
import com.example.smartmessaging.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatApiController {

    private final StatService statService;

    @GetMapping("/delivery")
    public StatPageResponse getDeliveryStats(@ModelAttribute StatSearchRequest request) {
        return statService.getDeliveryStats(request);
    }

    @GetMapping("/channel")
    public StatPageResponse getChannelStats(@ModelAttribute StatSearchRequest request) {
        return statService.getChannelStats(request);
    }

    @GetMapping("/cost")
    public StatPageResponse getCostStats(@ModelAttribute StatSearchRequest request) {
        return statService.getCostStats(request);
    }

    @GetMapping("/customer")
    public StatPageResponse getCustomerStats(@ModelAttribute StatSearchRequest request) {
        return statService.getCustomerStats(request);
    }

    @GetMapping("/performance")
    public StatPageResponse getPerformanceStats(@ModelAttribute StatSearchRequest request) {
        return statService.getPerformanceStats(request);
    }
}
