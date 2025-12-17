package com.novaswap.api.controller;

import com.novaswap.model.PoolStats;
import com.novaswap.service.DataAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Pool Statistics", description = "池统计和数据聚合API")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class PoolStatsController {
    
    private final DataAggregationService dataAggregationService;
    
    @Operation(summary = "获取池统计信息", description = "获取单个池的TVL、交易量、APY等统计数据")
    @GetMapping("/pool/{pairAddress}")
    public ResponseEntity<PoolStats> getPoolStats(@PathVariable String pairAddress) {
        return ResponseEntity.ok(dataAggregationService.getPoolStats(pairAddress));
    }
    
    @Operation(summary = "获取所有池统计", description = "获取所有池的统计数据列表")
    @GetMapping("/pools")
    public ResponseEntity<List<PoolStats>> getAllPoolStats() {
        return ResponseEntity.ok(dataAggregationService.getAllPoolStats());
    }
    
    @Operation(summary = "获取热门池（按TVL）", description = "获取TVL最高的池列表")
    @GetMapping("/pools/top-tvl")
    public ResponseEntity<List<PoolStats>> getTopPoolsByTVL(
        @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(dataAggregationService.getTopPoolsByTVL(limit));
    }
    
    @Operation(summary = "获取热门池（按交易量）", description = "获取24h交易量最高的池列表")
    @GetMapping("/pools/top-volume")
    public ResponseEntity<List<PoolStats>> getTopPoolsByVolume(
        @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(dataAggregationService.getTopPoolsByVolume(limit));
    }
    
    @Operation(summary = "获取池APY", description = "计算池的年化收益率")
    @GetMapping("/pool/{pairAddress}/apy")
    public ResponseEntity<Map<String, Object>> getPoolAPY(@PathVariable String pairAddress) {
        PoolStats stats = dataAggregationService.getPoolStats(pairAddress);
        
        Map<String, Object> response = new HashMap<>();
        response.put("pairAddress", pairAddress);
        response.put("apy", stats.getApy());
        response.put("tvl", stats.getTvlUsd());
        response.put("fees24h", stats.getFees24hUsd());
        response.put("volume24h", stats.getVolume24hUsd());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "刷新池统计", description = "手动触发池统计数据更新")
    @PostMapping("/pool/{pairAddress}/refresh")
    public ResponseEntity<Map<String, String>> refreshPoolStats(@PathVariable String pairAddress) {
        dataAggregationService.updatePoolStats(pairAddress);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Pool stats refresh initiated for " + pairAddress);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "清除统计缓存", description = "清除所有缓存的统计数据")
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        dataAggregationService.clearCache();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Cache cleared successfully");
        
        return ResponseEntity.ok(response);
    }
}
