package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.HistorySearchRequestDTO;
import com.example.smartmessaging.dto.response.HistoryChannelResponseDTO;
import com.example.smartmessaging.dto.response.HistoryDetailResponseDTO;
import com.example.smartmessaging.dto.response.HistoryFilterOptionDTO;
import com.example.smartmessaging.dto.response.HistoryListResponseDTO;
import com.example.smartmessaging.dto.response.HistoryStatusOptionDTO;
import com.example.smartmessaging.dto.response.HistoryTagResponseDTO;
import com.example.smartmessaging.dto.response.PageResponseDTO;
import com.example.smartmessaging.dto.type.SendHistoryStatus;
import com.example.smartmessaging.service.repository.HistoryMapper;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import com.example.smartmessaging.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryServiceImpl implements HistoryService {
    private final HistoryMapper historyMapper;

    // 검색 조건을 기반으로 전송 기록 목록과 페이지 정보를 반환
    @Override
    public PageResponseDTO<HistoryListResponseDTO> getHistories(HistorySearchRequestDTO condition) {
        condition.normalize();
        long totalElements = historyMapper.countHistories(condition);
        int totalPages = totalElements == 0 ? 0
                : (int) Math.ceil((double) totalElements / HistorySearchRequestDTO.PAGE_SIZE);

        // 요청 페이지가 실제 전체 페이지보다 클 경우 마지막 페이지로 가도록 함
        // url에 잘못된 값이 들어오는 것을 방지
        if (totalPages > 0 && condition.getPage() > totalPages) {
            condition.setPage(totalPages);
        }

        // 현재 페이지 목록 조회, 전체 개수 0일 경우 DB 조회하지 않고 빈 리스트로
        List<HistoryListResponseDTO> histories = totalElements == 0
                ? Collections.emptyList()
                : historyMapper.findHistories(condition);
        attachChannelsAndTags(histories);

        // 페이지 응답 DTO로 감싸서 반환
        return PageResponseDTO.of(histories, condition.getPage(), HistorySearchRequestDTO.PAGE_SIZE, totalElements);
    }

    @Override
    public HistoryDetailResponseDTO getHistoryDetail(Long sendHistoryId) {
        HistoryDetailResponseDTO detail = historyMapper.findHistoryDetailById(sendHistoryId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.SEND_HISTORY_NOT_FOUND);
        }

        List<Long> historyIds = List.of(sendHistoryId);
        detail.setChannels(historyMapper.findChannelsByHistoryIds(historyIds).stream()
                .map(HistoryChannelResponseDTO::getChannelName)
                .toList());
        detail.setTags(historyMapper.findTagsByHistoryIds(historyIds).stream()
                .map(HistoryTagResponseDTO::getTagName)
                .toList());
        detail.setAttemptFlows(historyMapper.findAttemptFlowsByHistoryId(sendHistoryId));
        return detail;
    }

    @Override
    public List<HistoryFilterOptionDTO> getChannelOptions() {
        return historyMapper.findChannelOptions();
    }

    @Override
    public List<HistoryFilterOptionDTO> getTagOptions() {
        return historyMapper.findTagOptions();
    }

    @Override
    public List<HistoryStatusOptionDTO> getStatusOptions() {
        return Arrays.stream(SendHistoryStatus.values())
                .map(status -> new HistoryStatusOptionDTO(status.getValue(), status.getLabel()))
                .toList();
    }

    @Override
    public List<String> getPurposeOptions() {
        return historyMapper.findPurposeOptions();
    }

    // 전송기록 목록에 채널과 태그를 붙이는 메서드
    private void attachChannelsAndTags(List<HistoryListResponseDTO> histories) {
        if (histories.isEmpty()) { // 목록이 비어있을 경우 바로 종료
            return;
        }

        // 리스트에 있는 전송기록 id 목록 추출
        List<Long> historyIds = histories.stream().map(HistoryListResponseDTO::getId).toList();

        // 전송 기록에 해당하는 채널 정보를 Map으로 묶음
        Map<Long, List<String>> channelsByHistoryId = historyMapper.findChannelsByHistoryIds(historyIds).stream()
                .collect(Collectors.groupingBy(HistoryChannelResponseDTO::getSendHistoryId,
                        Collectors.mapping(HistoryChannelResponseDTO::getChannelName, Collectors.toList())));

        // 전송 기록에 해당하는 태그 정보를 Map으로 묶음
        Map<Long, List<String>> tagsByHistoryId = historyMapper.findTagsByHistoryIds(historyIds).stream()
                .collect(Collectors.groupingBy(HistoryTagResponseDTO::getSendHistoryId,
                        Collectors.mapping(HistoryTagResponseDTO::getTagName, Collectors.toList())));

        // 각 히스토리에 채널/태그 세팅
        histories.forEach(history -> {
            history.setChannels(channelsByHistoryId.getOrDefault(history.getId(), Collections.emptyList()));
            history.setTags(tagsByHistoryId.getOrDefault(history.getId(), Collections.emptyList()));
        });
    }
}
