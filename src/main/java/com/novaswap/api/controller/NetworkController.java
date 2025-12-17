package com.novaswap.api.controller;

import com.novaswap.api.dto.NetworkResponse;
import com.novaswap.model.Network;
import com.novaswap.service.NetworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Network", description = "网络和钱包管理API")
@RestController
@RequestMapping("/api/network")
@RequiredArgsConstructor
public class NetworkController {
    
    private final NetworkService networkService;
    
    @Operation(summary = "获取支持的网络列表")
    @GetMapping("/supported")
    public ResponseEntity<List<Network>> getSupportedNetworks() {
        return ResponseEntity.ok(networkService.getSupportedNetworks());
    }
    
    @Operation(summary = "获取当前网络信息")
    @GetMapping("/current")
    public ResponseEntity<Network> getCurrentNetwork() {
        return ResponseEntity.ok(networkService.getCurrentNetwork());
    }
    
    @Operation(summary = "获取所有网络信息")
    @GetMapping("/all")
    public ResponseEntity<NetworkResponse> getAllNetworkInfo() {
        Network current = networkService.getCurrentNetwork();
        List<Network> supported = networkService.getSupportedNetworks();
        return ResponseEntity.ok(new NetworkResponse(current, supported));
    }
    
    @Operation(summary = "检查网络是否支持")
    @GetMapping("/check/{chainId}")
    public ResponseEntity<Map<String, Object>> checkNetworkSupport(@PathVariable Long chainId) {
        boolean supported = networkService.isNetworkSupported(chainId);
        Map<String, Object> response = new HashMap<>();
        response.put("chainId", chainId);
        response.put("supported", supported);
        if (supported) {
            response.put("network", networkService.getNetworkById(chainId));
        }
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "格式化地址（脱敏显示）")
    @GetMapping("/format-address")
    public ResponseEntity<Map<String, String>> formatAddress(@RequestParam String address) {
        Map<String, String> response = new HashMap<>();
        response.put("original", address);
        response.put("formatted", networkService.formatAddress(address));
        response.put("valid", String.valueOf(networkService.isValidAddress(address)));
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "验证地址格式")
    @GetMapping("/validate-address")
    public ResponseEntity<Map<String, Boolean>> validateAddress(@RequestParam String address) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", networkService.isValidAddress(address));
        return ResponseEntity.ok(response);
    }
}
