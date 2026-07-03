package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.HistorySearchRequestDTO;
import com.example.smartmessaging.dto.response.HistoryFilterOptionDTO;
import com.example.smartmessaging.dto.response.HistoryDetailResponseDTO;
import com.example.smartmessaging.dto.response.HistoryListResponseDTO;
import com.example.smartmessaging.dto.response.HistoryStatusOptionDTO;
import com.example.smartmessaging.dto.response.PageResponseDTO;

import java.util.List;

public interface HistoryService {
    PageResponseDTO<HistoryListResponseDTO> getHistories(HistorySearchRequestDTO condition);

    HistoryDetailResponseDTO getHistoryDetail(Long sendHistoryId);

    List<HistoryFilterOptionDTO> getChannelOptions();

    List<HistoryFilterOptionDTO> getTagOptions();

    List<HistoryStatusOptionDTO> getStatusOptions();

    List<String> getPurposeOptions();
}
