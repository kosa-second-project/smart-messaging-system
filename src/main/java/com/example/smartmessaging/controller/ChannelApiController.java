package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelApiController {

    private final ChannelService channelService;

    /**
     * 2단계 진입 시, 활성화된 채널 정보(이름, 단가 등) 조회
     */
    @GetMapping("/active")
    public ResponseEntity<List<ChannelVO>> getActiveChannels() {
        return ResponseEntity.ok(channelService.getActiveChannels());
    }
}
