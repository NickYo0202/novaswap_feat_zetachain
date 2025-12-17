package com.novaswap.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.protocol.Web3j;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Health", description = "健康检查API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {
    
    private final Web3j web3j;
    
    @Operation(summary = "健康检查", description = "检查服务状态")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().getEpochSecond());
        response.put("service", "NovaSwap Backend");
        response.put("version", "0.1.0");
        
        // 检查Web3连接
        try {
            web3j.web3ClientVersion().send();
            response.put("web3", "connected");
        } catch (Exception e) {
            response.put("web3", "disconnected");
            response.put("status", "DEGRADED");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "服务信息", description = "获取服务基本信息")
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "NovaSwap Backend API");
        response.put("version", "0.1.0-SNAPSHOT");
        response.put("description", "DEX Backend Services with Multi-chain Support");
        response.put("features", Map.of(
            "multichain", true,
            "swap", true,
            "liquidity", true,
            "statistics", true,
            "events", true,
            "priceHistory", true,
            "cache", true
        ));
        
        return ResponseEntity.ok(response);
    }
}
