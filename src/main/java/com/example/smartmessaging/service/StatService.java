package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.StatPageResponse;

public interface StatService {

    StatPageResponse getDeliveryStats(StatSearchRequest request);

    StatPageResponse getChannelStats(StatSearchRequest request);

    StatPageResponse getCostStats(StatSearchRequest request);

    StatPageResponse getCustomerStats(StatSearchRequest request);

    StatPageResponse getPerformanceStats(StatSearchRequest request);
}
