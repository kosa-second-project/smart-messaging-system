package com.example.smartmessaging.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CheckDbController {
    private final JdbcTemplate jdbcTemplate;
    @GetMapping("/api/check-db")
    public String checkDb() {
        try {
            return jdbcTemplate.queryForObject("SELECT search_condition FROM user_constraints WHERE constraint_name = 'CK_CUSTOMER_TYPE'", String.class);
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
