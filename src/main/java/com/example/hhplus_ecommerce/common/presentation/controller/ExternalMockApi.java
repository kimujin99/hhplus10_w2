package com.example.hhplus_ecommerce.common.presentation.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/external")
@Slf4j
public class ExternalMockApi {
    @PostMapping("/orders")
    public Map<String, String> mockReceive(@RequestBody Map<String, Object> order) {
        log.info("[Mock External API] Received: {}", order);
        return Map.of("status", "ok");
    }
}