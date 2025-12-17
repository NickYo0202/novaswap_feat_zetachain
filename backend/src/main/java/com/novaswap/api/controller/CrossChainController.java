package com.novaswap.api.controller;

import com.novaswap.api.dto.crosschain.*;
import com.novaswap.model.crosschain.CrossChainRoute;
import com.novaswap.model.crosschain.CrossChainTransaction;
import com.novaswap.service.crosschain.CrossChainBridgeService;
import com.novaswap.service.crosschain.CrossChainFeeService;
import com.novaswap.service.crosschain.CrossChainRouteService;
import com.novaswap.service.crosschain.CrossChainTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 跨链桥接API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/crosschain")
@RequiredArgsConstructor
@Tag(name = "Cross-Chain Bridge", description = "Cross-chain swap and bridge operations")
public class CrossChainController {

    private final CrossChainRouteService routeService;
    private final CrossChainFeeService feeService;
    private final CrossChainBridgeService bridgeService;
    private final CrossChainTransactionService transactionService;

    /**
     * 搜索跨链路由
     */
    @PostMapping("/routes")
    @Operation(summary = "Search cross-chain routes", 
               description = "Find optimal routes for cross-chain swaps")
    public ResponseEntity<CrossChainRouteResponse> searchRoutes(
            @Valid @RequestBody CrossChainRouteRequest request) {
        
        try {
            log.info("Searching cross-chain routes: {} -> {}", 
                    request.getSourceChainId(), request.getTargetChainId());
            
            List<CrossChainRoute> routes = routeService.searchRoutes(
                    request.getSourceChainId(),
                    request.getTargetChainId(),
                    request.getSourceTokenAddress(),
                    request.getTargetTokenAddress(),
                    request.getAmountIn(),
                    request.getRouteType()
            );
            
            if (routes.isEmpty()) {
                return ResponseEntity.ok(CrossChainRouteResponse.builder()
                        .success(false)
                        .message("No cross-chain route found")
                        .routes(routes)
                        .routeCount(0)
                        .build());
            }
            
            return ResponseEntity.ok(CrossChainRouteResponse.builder()
                    .success(true)
                    .message("Routes found successfully")
                    .routes(routes)
                    .routeCount(routes.size())
                    .build());
            
        } catch (Exception e) {
            log.error("Error searching routes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CrossChainRouteResponse.builder()
                            .success(false)
                            .message("Error searching routes: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 估算跨链费用
     */
    @PostMapping("/estimate-fee")
    @Operation(summary = "Estimate cross-chain fees",
               description = "Calculate total fees for a cross-chain transaction")
    public ResponseEntity<CrossChainFeeEstimateResponse> estimateFee(
            @Valid @RequestBody CrossChainRouteRequest request) {
        
        try {
            log.info("Estimating cross-chain fee: {} -> {}",
                    request.getSourceChainId(), request.getTargetChainId());
            
            List<CrossChainRoute> routes = routeService.searchRoutes(
                    request.getSourceChainId(),
                    request.getTargetChainId(),
                    request.getSourceTokenAddress(),
                    request.getTargetTokenAddress(),
                    request.getAmountIn(),
                    CrossChainRoute.RouteType.CHEAPEST
            );
            
            if (routes.isEmpty()) {
                return ResponseEntity.ok(CrossChainFeeEstimateResponse.builder()
                        .success(false)
                        .message("No route found to estimate fee")
                        .build());
            }
            
            CrossChainRoute.FeeBreakdown feeBreakdown = routes.get(0).getFeeBreakdown();
            
            return ResponseEntity.ok(CrossChainFeeEstimateResponse.builder()
                    .success(true)
                    .message("Fee estimated successfully")
                    .sourceChainGasFee(feeBreakdown.getSourceChainGasFee())
                    .bridgeFee(feeBreakdown.getBridgeFee())
                    .targetChainGasFee(feeBreakdown.getTargetChainGasFee())
                    .serviceFee(feeBreakdown.getServiceFee())
                    .thirdPartyFee(feeBreakdown.getThirdPartyFee())
                    .totalFee(feeBreakdown.getTotalFee())
                    .feeBreakdownDescription(feeService.getFeeBreakdownDescription(feeBreakdown))
                    .build());
            
        } catch (Exception e) {
            log.error("Error estimating fee: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CrossChainFeeEstimateResponse.builder()
                            .success(false)
                            .message("Error estimating fee: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 执行跨链swap
     */
    @PostMapping("/swap")
    @Operation(summary = "Execute cross-chain swap",
               description = "Initiate a cross-chain swap transaction")
    public ResponseEntity<CrossChainSwapResponse> executeSwap(
            @Valid @RequestBody CrossChainSwapRequest request) {
        
        try {
            log.info("Executing cross-chain swap for user: {}", request.getUserAddress());
            
            // 搜索最优路由
            List<CrossChainRoute> routes = routeService.searchRoutes(
                    request.getSourceChainId(),
                    request.getTargetChainId(),
                    request.getSourceTokenAddress(),
                    request.getTargetTokenAddress(),
                    new BigInteger(request.getAmountIn()),
                    CrossChainRoute.RouteType.BALANCED
            );
            
            if (routes.isEmpty()) {
                return ResponseEntity.ok(CrossChainSwapResponse.builder()
                        .success(false)
                        .message("No route available for this swap")
                        .build());
            }
            
            CrossChainRoute selectedRoute = routes.get(0);
            
            // 执行swap
            String transactionId = bridgeService.executeCrossChainSwap(
                    selectedRoute,
                    request.getUserAddress(),
                    request.getSlippagePercent()
            );
            
            return ResponseEntity.ok(CrossChainSwapResponse.builder()
                    .success(true)
                    .message("Cross-chain swap initiated successfully")
                    .transactionId(transactionId)
                    .estimatedTimeSeconds(selectedRoute.getEstimatedTimeSeconds().toString())
                    .build());
            
        } catch (Exception e) {
            log.error("Error executing swap: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CrossChainSwapResponse.builder()
                            .success(false)
                            .message("Error executing swap: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 查询交易状态
     */
    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get transaction status",
               description = "Query the status of a cross-chain transaction")
    public ResponseEntity<CrossChainTransactionStatusResponse> getTransactionStatus(
            @PathVariable String transactionId) {
        
        try {
            CrossChainTransaction transaction = transactionService.getTransaction(transactionId);
            
            if (transaction == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CrossChainTransactionStatusResponse.builder()
                                .success(false)
                                .message("Transaction not found")
                                .build());
            }
            
            return ResponseEntity.ok(buildTransactionStatusResponse(transaction));
            
        } catch (Exception e) {
            log.error("Error getting transaction status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CrossChainTransactionStatusResponse.builder()
                            .success(false)
                            .message("Error getting transaction status: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 查询用户交易历史
     */
    @GetMapping("/transactions/user/{userAddress}")
    @Operation(summary = "Get user transaction history",
               description = "Get all cross-chain transactions for a user")
    public ResponseEntity<List<CrossChainTransactionStatusResponse>> getUserTransactions(
            @PathVariable String userAddress) {
        
        try {
            List<CrossChainTransaction> transactions = transactionService.getUserTransactions(userAddress);
            
            List<CrossChainTransactionStatusResponse> responses = transactions.stream()
                    .map(this::buildTransactionStatusResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            log.error("Error getting user transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 重试失败的交易
     */
    @PostMapping("/transaction/{transactionId}/retry")
    @Operation(summary = "Retry failed transaction",
               description = "Retry a failed cross-chain transaction")
    public ResponseEntity<CrossChainSwapResponse> retryTransaction(
            @PathVariable String transactionId) {
        
        try {
            bridgeService.retryTransaction(transactionId);
            
            return ResponseEntity.ok(CrossChainSwapResponse.builder()
                    .success(true)
                    .message("Transaction retry initiated")
                    .transactionId(transactionId)
                    .build());
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CrossChainSwapResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CrossChainSwapResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error retrying transaction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CrossChainSwapResponse.builder()
                            .success(false)
                            .message("Error retrying transaction: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 获取支持的桥接路径
     */
    @GetMapping("/supported-paths")
    @Operation(summary = "Get supported bridge paths",
               description = "Get all supported cross-chain bridge paths")
    public ResponseEntity<SupportedBridgePathsResponse> getSupportedPaths() {
        
        try {
            // 这里应该返回所有支持的桥接路径
            // 简化版本：返回几个主要的路径
            List<SupportedBridgePathsResponse.BridgePath> paths = List.of(
                    SupportedBridgePathsResponse.BridgePath.builder()
                            .sourceChainId(1)
                            .sourceChainName("Ethereum")
                            .targetChainId(56)
                            .targetChainName("BSC")
                            .available(bridgeService.isRouteAvailable(1, 56))
                            .estimatedTimeSeconds(180L)
                            .baseFee("5000000")
                            .build(),
                    SupportedBridgePathsResponse.BridgePath.builder()
                            .sourceChainId(1)
                            .sourceChainName("Ethereum")
                            .targetChainId(137)
                            .targetChainName("Polygon")
                            .available(bridgeService.isRouteAvailable(1, 137))
                            .estimatedTimeSeconds(240L)
                            .baseFee("3000000")
                            .build(),
                    SupportedBridgePathsResponse.BridgePath.builder()
                            .sourceChainId(56)
                            .sourceChainName("BSC")
                            .targetChainId(137)
                            .targetChainName("Polygon")
                            .available(bridgeService.isRouteAvailable(56, 137))
                            .estimatedTimeSeconds(300L)
                            .baseFee("2000000")
                            .build()
            );
            
            return ResponseEntity.ok(SupportedBridgePathsResponse.builder()
                    .success(true)
                    .message("Supported paths retrieved successfully")
                    .paths(paths)
                    .build());
            
        } catch (Exception e) {
            log.error("Error getting supported paths: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SupportedBridgePathsResponse.builder()
                            .success(false)
                            .message("Error getting supported paths: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 构建交易状态响应
     */
    private CrossChainTransactionStatusResponse buildTransactionStatusResponse(
            CrossChainTransaction transaction) {
        
        List<CrossChainTransactionStatusResponse.StatusHistoryDto> historyDtos = 
                transaction.getStatusHistory().stream()
                        .map(h -> CrossChainTransactionStatusResponse.StatusHistoryDto.builder()
                                .status(h.getStatus())
                                .timestamp(h.getTimestamp())
                                .description(h.getDescription())
                                .build())
                        .collect(Collectors.toList());
        
        return CrossChainTransactionStatusResponse.builder()
                .success(true)
                .message("Transaction status retrieved successfully")
                .transactionId(transaction.getTransactionId())
                .status(transaction.getStatus())
                .sourceChainId(transaction.getSourceChainId())
                .targetChainId(transaction.getTargetChainId())
                .sourceTokenAddress(transaction.getSourceTokenAddress())
                .targetTokenAddress(transaction.getTargetTokenAddress())
                .amountIn(transaction.getAmountIn())
                .amountOut(transaction.getAmountOut())
                .sourceTxHash(transaction.getSourceTxHash())
                .targetTxHash(transaction.getTargetTxHash())
                .bridgeMessageId(transaction.getBridgeMessageId())
                .sourceExplorerLink(getExplorerLink(transaction.getSourceChainId(), 
                        transaction.getSourceTxHash()))
                .targetExplorerLink(getExplorerLink(transaction.getTargetChainId(), 
                        transaction.getTargetTxHash()))
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .estimatedTimeSeconds(transaction.getEstimatedTimeSeconds())
                .actualTimeSeconds(transaction.getActualTimeSeconds())
                .retryCount(transaction.getRetryCount())
                .retryable(transaction.isRetryable())
                .errorMessage(transaction.getErrorMessage())
                .statusHistory(historyDtos)
                .build();
    }

    /**
     * 获取区块浏览器链接
     */
    private String getExplorerLink(Integer chainId, String txHash) {
        if (txHash == null) {
            return null;
        }
        
        String template = switch (chainId) {
            case 1 -> "https://etherscan.io/tx/%s";
            case 56 -> "https://bscscan.com/tx/%s";
            case 137 -> "https://polygonscan.com/tx/%s";
            case 42161 -> "https://arbiscan.io/tx/%s";
            case 10 -> "https://optimistic.etherscan.io/tx/%s";
            default -> "https://etherscan.io/tx/%s";
        };
        
        return String.format(template, txHash);
    }
}
