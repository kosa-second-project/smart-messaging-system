package com.example.smartmessaging.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "대시보드");
        return "pages/dashboard";
    }

    @GetMapping("/send")
    public String sendRedirect() {
        return "redirect:/send/recipients";
    }

    @GetMapping("/send/recipients")
    public String sendRecipients(Model model) {
        model.addAttribute("pageTitle", "메시지 발송");
        model.addAttribute("activeStep", 1);
        return "pages/send/recipients";
    }

    @GetMapping("/send/message")
    public String sendMessage(Model model) {
        model.addAttribute("pageTitle", "메시지 발송");
        model.addAttribute("activeStep", 2);
        return "pages/send/message";
    }

    @GetMapping("/send/review")
    public String sendReview(Model model) {
        model.addAttribute("pageTitle", "메시지 발송");
        model.addAttribute("activeStep", 3);
        return "pages/send/review";
    }

    @GetMapping("/templates")
    public String templates(Model model) {
        model.addAttribute("pageTitle", "템플릿 관리");
        return "pages/templates";
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("pageTitle", "고객 관리");
        return "pages/customers";
    }

    @GetMapping("/stats/delivery")
    public String statsDelivery(Model model) {
        model.addAttribute("pageTitle", "발송 현황");
        return "pages/stats/stats-delivery";
    }

    @GetMapping("/stats/channel")
    public String statsChannel(Model model) {
        model.addAttribute("pageTitle", "채널 분석");
        return "pages/stats/stats-channel";
    }

    @GetMapping("/stats/cost")
    public String statsCost(Model model) {
        model.addAttribute("pageTitle", "비용 분석");
        return "pages/stats/stats-cost";
    }

    @GetMapping("/stats/customer")
    public String statsCustomer(Model model) {
        model.addAttribute("pageTitle", "고객 분석");
        return "pages/stats/stats-customer";
    }

    @GetMapping("/stats/performance")
    public String statsPerformance(Model model) {
        model.addAttribute("pageTitle", "성과 분석");
        return "pages/stats/stats-performance";
    }
}
