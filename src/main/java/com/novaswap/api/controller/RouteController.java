package com.novaswap.api.controller;

import com.novaswap.api.dto.RouteSearchRequest;
import com.novaswap.model.RouteInfo;
import com.novaswap.service.RouteSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Route", description = "路由搜索API")
@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
public class RouteController {
    
    private final RouteSearchService routeSearchService;
    
    @Operation(summary = "搜索最优路由", description = "搜索从tokenIn到tokenOut的最优兑换路由，支持直达和多跳")
    @PostMapping("/search")
    public ResponseEntity<RouteInfo> searchBestRoute(@Valid @RequestBody RouteSearchRequest request) {
        RouteInfo route = routeSearchService.findBestRoute(
            request.getTokenIn(),
            request.getTokenOut(),
            request.getAmountIn(),
            request.getSlippageTolerance(),
            request.getIntermediateTokens()
        );
        
        return ResponseEntity.ok(route);
    }
    
    @Operation(summary = "获取兑换预估", description = "预估兑换输出、价格影响和最小接收量")
    @PostMapping("/quote")
    public ResponseEntity<Map<String, Object>> getSwapQuote(@Valid @RequestBody RouteSearchRequest request) {
        RouteInfo route = routeSearchService.findBestRoute(
            request.getTokenIn(),
            request.getTokenOut(),
            request.getAmountIn(),
            request.getSlippageTolerance(),
            request.getIntermediateTokens()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("path", route.getPath());
        response.put("amountOut", route.getAmountOut().toString());
        response.put("minAmountOut", route.getMinAmountOut().toString());
        response.put("priceImpact", route.getPriceImpact().toString() + "%");
        response.put("isDirect", route.isDirect());
        response.put("hops", route.getHops());
        response.put("slippageTolerance", request.getSlippageTolerance() * 100 + "%");
        
        // 判断价格影响是否过高
        boolean highPriceImpact = route.getPriceImpact().doubleValue() > 5.0;
        response.put("highPriceImpact", highPriceImpact);
        if (highPriceImpact) {
            response.put("warning", "Price impact is higher than 5%, consider reducing trade size");
        }
        
        return ResponseEntity.ok(response);
    }
}
