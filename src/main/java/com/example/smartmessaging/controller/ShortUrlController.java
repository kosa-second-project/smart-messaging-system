package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.vo.ShortUrlTargetVO;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.service.ShortUrlService;
import com.example.smartmessaging.util.MaskingUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    @GetMapping("/r/{code}")
    public String redirectTrackedUrl(@PathVariable String code, Model model) {
        try {
            String redirectUrl = shortUrlService.markClickAndResolveRedirect(code);
            return "redirect:" + redirectUrl;
        } catch (BusinessException e) {
            model.addAttribute("message", e.getMessage());
            return "pages/unsubscribe/invalid";
        }
    }

    @GetMapping("/u/{code}")
    public String unsubscribeConfirm(@PathVariable String code, Model model) {
        try {
            ShortUrlTargetVO target = shortUrlService.getUnsubscribeTarget(code);
            model.addAttribute("code", code);
            model.addAttribute("customerName", target.getCustomerName());
            model.addAttribute("phone", MaskingUtils.maskPhone(target.getPhone()));
            return "pages/unsubscribe/confirm";
        } catch (BusinessException e) {
            model.addAttribute("message", e.getMessage());
            return "pages/unsubscribe/invalid";
        }
    }

    @PostMapping("/u/{code}/unsubscribe")
    public String unsubscribe(@PathVariable String code, Model model) {
        try {
            ShortUrlTargetVO target = shortUrlService.unsubscribe(code);
            model.addAttribute("customerName", target.getCustomerName());
            return "pages/unsubscribe/complete";
        } catch (BusinessException e) {
            model.addAttribute("message", e.getMessage());
            return "pages/unsubscribe/invalid";
        }
    }
}
