package com.novaswap.api.controller;

import com.novaswap.api.dto.BalanceRequest;
import com.novaswap.model.TokenBalance;
import com.novaswap.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Balance", description = "余额查询API")
@RestController
@RequestMapping("/api/balance")
@RequiredArgsConstructor
public class BalanceController {
    
    private final BalanceService balanceService;
    
    @Operation(summary = "获取ETH余额")
    @GetMapping("/eth/{address}")
    public ResponseEntity<Map<String, Object>> getEthBalance(@PathVariable String address) {
        BigInteger balance = balanceService.getEthBalance(address);
        Map<String, Object> response = new HashMap<>();
        response.put("address", address);
        response.put("balance", balance.toString());
        response.put("balanceInEth", balance.divide(BigInteger.TEN.pow(18)).toString());
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "获取单个ERC20代币余额")
    @GetMapping("/token")
    public ResponseEntity<TokenBalance> getTokenBalance(
        @RequestParam String tokenAddress,
        @RequestParam String userAddress
    ) {
        return ResponseEntity.ok(balanceService.getTokenInfo(tokenAddress, userAddress));
    }
    
    @Operation(summary = "批量获取多个代币余额")
    @PostMapping("/multiple")
    public ResponseEntity<List<TokenBalance>> getMultipleBalances(
        @Valid @RequestBody BalanceRequest request
    ) {
        return ResponseEntity.ok(
            balanceService.getMultipleTokenBalances(
                request.getTokenAddresses(), 
                request.getUserAddress()
            )
        );
    }
}
