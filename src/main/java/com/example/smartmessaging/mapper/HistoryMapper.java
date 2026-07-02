package com.example.smartmessaging.mapper;

import com.example.smartmessaging.dto.request.HistorySearchRequestDTO;
import com.example.smartmessaging.dto.response.HistoryChannelResponseDTO;
import com.example.smartmessaging.dto.response.HistoryFilterOptionDTO;
import com.example.smartmessaging.dto.response.HistoryListResponseDTO;
import com.example.smartmessaging.dto.response.HistoryTagResponseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HistoryMapper {
    long countHistories(HistorySearchRequestDTO condition);
    List<HistoryListResponseDTO> findHistories(HistorySearchRequestDTO condition);
    List<HistoryChannelResponseDTO> findChannelsByHistoryIds(@Param("historyIds") List<Long> historyIds);
    List<HistoryTagResponseDTO> findTagsByHistoryIds(@Param("historyIds") List<Long> historyIds);
    List<HistoryFilterOptionDTO> findChannelOptions();
    List<HistoryFilterOptionDTO> findTagOptions();
    List<String> findStatusOptions();
    List<String> findPurposeOptions();
}
