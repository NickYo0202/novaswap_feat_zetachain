package com.novaswap.api.controller;

import com.novaswap.api.dto.PriceHistoryRequest;
import com.novaswap.model.PricePoint;
import com.novaswap.service.PriceHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Price History", description = "价格历史和K线数据API")
@RestController
@RequestMapping("/api/price")
@RequiredArgsConstructor
public class PriceHistoryController {
    
    private final PriceHistoryService priceHistoryService;
    
    @Operation(summary = "获取价格历史", description = "获取指定时间范围和粒度的价格K线数据")
    @PostMapping("/history")
    public ResponseEntity<List<PricePoint>> getPriceHistory(
        @Valid @RequestBody PriceHistoryRequest request
    ) {
        Instant startTime = request.getStartTime() != null 
            ? Instant.ofEpochSecond(request.getStartTime())
            : Instant.now().minusSeconds(86400); // 默认24小时前
        
        Instant endTime = request.getEndTime() != null
            ? Instant.ofEpochSecond(request.getEndTime())
            : Instant.now();
        
        String interval = request.getInterval() != null ? request.getInterval() : "1h";
        
        List<PricePoint> history = priceHistoryService.getPriceHistory(
            request.getPairAddress(),
            interval,
            startTime,
            endTime
        );
        
        // 如果指定了limit，截取结果
        if (request.getLimit() != null && request.getLimit() > 0 && history.size() > request.getLimit()) {
            history = history.subList(Math.max(0, history.size() - request.getLimit()), history.size());
        }
        
        return ResponseEntity.ok(history);
    }
    
    @Operation(summary = "获取支持的时间间隔", description = "获取K线图支持的所有时间间隔")
    @GetMapping("/intervals")
    public ResponseEntity<Map<String, Object>> getSupportedIntervals() {
        List<String> intervals = priceHistoryService.getSupportedIntervals();
        
        Map<String, Object> response = new HashMap<>();
        response.put("intervals", intervals);
        response.put("default", "1h");
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "获取最新价格", description = "获取池的当前价格")
    @GetMapping("/{pairAddress}/current")
    public ResponseEntity<Map<String, Object>> getCurrentPrice(
        @PathVariable String pairAddress
    ) {
        // 实际应从链上查询最新储备
        // 这里返回模拟数据
        Map<String, Object> response = new HashMap<>();
        response.put("pairAddress", pairAddress);
        response.put("price", "1000.50");
        response.put("timestamp", Instant.now().getEpochSecond());
        
        return ResponseEntity.ok(response);
    }
}
