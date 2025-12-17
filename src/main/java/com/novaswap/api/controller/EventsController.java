package com.novaswap.api.controller;

import com.novaswap.model.event.BurnEvent;
import com.novaswap.model.event.MintEvent;
import com.novaswap.model.event.SwapEvent;
import com.novaswap.service.EventListenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Events", description = "链上事件查询API")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventsController {
    
    private final EventListenerService eventListenerService;
    
    @Operation(summary = "获取Swap事件", description = "获取指定池在指定区块范围内的Swap事件")
    @GetMapping("/swap")
    public ResponseEntity<List<SwapEvent>> getSwapEvents(
        @RequestParam String pairAddress,
        @RequestParam(required = false) Long fromBlock,
        @RequestParam(required = false) Long toBlock
    ) {
        BigInteger from = fromBlock != null ? BigInteger.valueOf(fromBlock) : BigInteger.ZERO;
        BigInteger to = toBlock != null 
            ? BigInteger.valueOf(toBlock) 
            : eventListenerService.getCurrentBlockNumber();
        
        return ResponseEntity.ok(eventListenerService.getSwapEvents(pairAddress, from, to));
    }
    
    @Operation(summary = "获取Mint事件", description = "获取指定池在指定区块范围内的Mint事件（添加流动性）")
    @GetMapping("/mint")
    public ResponseEntity<List<MintEvent>> getMintEvents(
        @RequestParam String pairAddress,
        @RequestParam(required = false) Long fromBlock,
        @RequestParam(required = false) Long toBlock
    ) {
        BigInteger from = fromBlock != null ? BigInteger.valueOf(fromBlock) : BigInteger.ZERO;
        BigInteger to = toBlock != null 
            ? BigInteger.valueOf(toBlock) 
            : eventListenerService.getCurrentBlockNumber();
        
        return ResponseEntity.ok(eventListenerService.getMintEvents(pairAddress, from, to));
    }
    
    @Operation(summary = "获取Burn事件", description = "获取指定池在指定区块范围内的Burn事件（移除流动性）")
    @GetMapping("/burn")
    public ResponseEntity<List<BurnEvent>> getBurnEvents(
        @RequestParam String pairAddress,
        @RequestParam(required = false) Long fromBlock,
        @RequestParam(required = false) Long toBlock
    ) {
        BigInteger from = fromBlock != null ? BigInteger.valueOf(fromBlock) : BigInteger.ZERO;
        BigInteger to = toBlock != null 
            ? BigInteger.valueOf(toBlock) 
            : eventListenerService.getCurrentBlockNumber();
        
        return ResponseEntity.ok(eventListenerService.getBurnEvents(pairAddress, from, to));
    }
    
    @Operation(summary = "获取当前区块号", description = "获取当前最新的区块号")
    @GetMapping("/block/current")
    public ResponseEntity<Map<String, Object>> getCurrentBlock() {
        BigInteger blockNumber = eventListenerService.getCurrentBlockNumber();
        
        Map<String, Object> response = new HashMap<>();
        response.put("blockNumber", blockNumber.toString());
        response.put("timestamp", System.currentTimeMillis() / 1000);
        
        return ResponseEntity.ok(response);
    }
}
