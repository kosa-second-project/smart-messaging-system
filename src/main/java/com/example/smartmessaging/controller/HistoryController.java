package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.HistorySearchRequestDTO;
import com.example.smartmessaging.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
@RequiredArgsConstructor
public class HistoryController {
    private final HistoryService historyService;

    @GetMapping("/history")
    public String history(@ModelAttribute("searchCondition") HistorySearchRequestDTO searchCondition, Model model) {
        model.addAttribute("pageTitle", "전송 기록");

        // 사용자가 선택한 검색 조건 searchCondition을 기반으로 전송 기록 목록과 페이지 정보를 조회
        model.addAttribute("historyPage", historyService.getHistories(searchCondition));

        // 검색 필터에 표시할 선택지 목록
        model.addAttribute("channels", historyService.getChannelOptions());
        model.addAttribute("tags", historyService.getTagOptions());
        model.addAttribute("statuses", historyService.getStatusOptions());
        model.addAttribute("purposes", historyService.getPurposeOptions());

        return "pages/history";
    }
}
