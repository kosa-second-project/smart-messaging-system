package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.HistorySearchRequestDTO;
import com.example.smartmessaging.dto.response.HistoryDetailResponseDTO;
import com.example.smartmessaging.dto.response.HistoryRetryFailedResponseDTO;
import com.example.smartmessaging.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HistoryController {
    private final HistoryService historyService;

    @GetMapping("/history")
    public String history(@ModelAttribute("searchCondition") HistorySearchRequestDTO searchCondition, Model model) {
        HistorySearchRequestDTO normalizedCondition = searchCondition.normalized();

        log.info("[HistoryController] 전송 기록 목록 조회 요청 - page: {}, sort: {}",
                normalizedCondition.page(), normalizedCondition.sort());

        model.addAttribute("pageTitle", "전송 기록");
        model.addAttribute("searchCondition", normalizedCondition);

        // 사용자가 선택한 검색 조건 searchCondition을 기반으로 전송 기록 목록과 페이지 정보를 조회
        model.addAttribute("historyPage", historyService.getHistories(normalizedCondition));

        // 검색 필터에 표시할 선택지 목록
        model.addAttribute("channels", historyService.getChannelOptions());
        model.addAttribute("tags", historyService.getTagOptions());
        model.addAttribute("statuses", historyService.getStatusOptions());
        model.addAttribute("purposes", historyService.getPurposeOptions());

        return "pages/history";
    }

    @GetMapping(value = "/history/{sendHistoryId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public HistoryDetailResponseDTO getHistoryDetail(@PathVariable Long sendHistoryId) {
        return historyService.getHistoryDetail(sendHistoryId);
    }

    @PostMapping(value = "/history/{sendHistoryId}/retry-failed", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public HistoryRetryFailedResponseDTO retryFailedTargets(@PathVariable Long sendHistoryId) {
        return historyService.retryFailedTargets(sendHistoryId);
    }
}
