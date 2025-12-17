package com.novaswap.api.controller;

import com.novaswap.api.dto.AllowanceCheckRequest;
import com.novaswap.service.AllowanceManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Allowance", description = "授权管理API")
@RestController
@RequestMapping("/api/allowance")
@RequiredArgsConstructor
public class AllowanceController {
    
    private final AllowanceManagementService allowanceService;
    
    @Operation(summary = "检查授权额度")
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkAllowance(
        @RequestParam String tokenAddress,
        @RequestParam String owner,
        @RequestParam String spender
    ) {
        BigInteger allowance = allowanceService.checkAllowance(tokenAddress, owner, spender);
        Map<String, Object> response = new HashMap<>();
        response.put("tokenAddress", tokenAddress);
        response.put("owner", owner);
        response.put("spender", spender);
        response.put("allowance", allowance.toString());
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "检查是否需要授权")
    @PostMapping("/needs-approval")
    public ResponseEntity<Map<String, Object>> needsApproval(
        @Valid @RequestBody AllowanceCheckRequest request
    ) {
        boolean needsApproval = allowanceService.needsApproval(
            request.getTokenAddress(),
            request.getOwner(),
            request.getSpender(),
            request.getAmount()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("needsApproval", needsApproval);
        response.put("currentAllowance", allowanceService.checkAllowance(
            request.getTokenAddress(), request.getOwner(), request.getSpender()
        ).toString());
        response.put("requiredAmount", request.getAmount().toString());
        
        if (needsApproval) {
            response.put("approveCalldata", allowanceService.buildInfiniteApproveCalldata(request.getSpender()));
        }
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "生成授权交易数据")
    @PostMapping("/build-approve")
    public ResponseEntity<Map<String, String>> buildApproveCalldata(
        @RequestParam String spender,
        @RequestParam(required = false) BigInteger amount,
        @RequestParam(defaultValue = "true") boolean infinite
    ) {
        String calldata;
        if (infinite) {
            calldata = allowanceService.buildInfiniteApproveCalldata(spender);
        } else {
            if (amount == null) {
                throw new IllegalArgumentException("Amount is required for non-infinite approval");
            }
            calldata = allowanceService.buildApproveCalldata(spender, amount);
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("calldata", calldata);
        response.put("spender", spender);
        response.put("infinite", String.valueOf(infinite));
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "检查双币授权状态（用于添加流动性）")
    @PostMapping("/check-dual")
    public ResponseEntity<Map<String, Boolean>> checkDualTokenApproval(
        @RequestParam String token0,
        @RequestParam String token1,
        @RequestParam String owner,
        @RequestParam String spender,
        @RequestParam BigInteger amount0,
        @RequestParam BigInteger amount1
    ) {
        return ResponseEntity.ok(
            allowanceService.checkDualTokenApproval(token0, token1, owner, spender, amount0, amount1)
        );
    }
}
