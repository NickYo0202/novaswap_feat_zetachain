package com.novaswap.service.crosschain;

import com.novaswap.model.crosschain.CrossChainRoute;
import com.novaswap.model.crosschain.CrossChainTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 跨链桥接主服务
 * 编排整个跨链流程：源链swap → 桥接 → 目标链swap
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrossChainBridgeService {

    private final CrossChainRouteService routeService;
    private final CrossChainFeeService feeService;
    private final CrossChainTransactionService transactionService;
    private final ZetaChainService zetaChainService;
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 5000;

    /**
     * 执行跨链交易
     * @param route 跨链路由
     * @param userAddress 用户地址
     * @param slippagePercent 滑点容忍度
     * @return 交易ID
     */
    public String executeCrossChainSwap(
            CrossChainRoute route,
            String userAddress,
            double slippagePercent) {
        
        // 验证路由
        if (!routeService.validateRoute(route)) {
            throw new IllegalArgumentException("Invalid or unavailable route");
        }
        
        // 创建交易记录
        CrossChainTransaction transaction = transactionService.createTransaction(
                route.getSourceChainId(),
                route.getTargetChainId(),
                route.getSourceTokenAddress(),
                route.getTargetTokenAddress(),
                route.getAmountIn().toString(),
                userAddress
        );
        
        log.info("Starting cross-chain swap for transaction: {}", transaction.getTransactionId());
        
        // 异步执行跨链流程
        CompletableFuture.runAsync(() -> {
            try {
                executeSwapFlow(transaction, route, slippagePercent);
            } catch (Exception e) {
                log.error("Error executing cross-chain swap: {}", e.getMessage(), e);
                transactionService.failTransaction(
                        transaction.getTransactionId(),
                        "Execution error: " + e.getMessage()
                );
            }
        });
        
        return transaction.getTransactionId();
    }

    /**
     * 执行swap流程
     */
    private void executeSwapFlow(
            CrossChainTransaction transaction,
            CrossChainRoute route,
            double slippagePercent) throws Exception {
        
        List<CrossChainRoute.RouteStep> steps = route.getSteps();
        String currentTxHash = null;
        
        for (int i = 0; i < steps.size(); i++) {
            CrossChainRoute.RouteStep step = steps.get(i);
            
            log.info("Executing step {}/{} for transaction {}: {}",
                    i + 1, steps.size(), transaction.getTransactionId(), step.getDescription());
            
            try {
                switch (step.getType()) {
                    case SWAP:
                        currentTxHash = executeSwapStep(step, transaction, slippagePercent);
                        break;
                    case BRIDGE:
                        currentTxHash = executeBridgeStep(step, transaction);
                        break;
                    case RECEIVE:
                        currentTxHash = waitForReceive(step, transaction);
                        break;
                }
                
                // 更新交易哈希
                if (step.getChainId().equals(route.getSourceChainId())) {
                    transactionService.updateSourceTxHash(transaction.getTransactionId(), currentTxHash);
                } else if (step.getChainId().equals(route.getTargetChainId())) {
                    transactionService.updateTargetTxHash(transaction.getTransactionId(), currentTxHash);
                }
                
            } catch (Exception e) {
                log.error("Step {} failed for transaction {}: {}",
                        i + 1, transaction.getTransactionId(), e.getMessage());
                
                // 尝试退款
                handleFailureAndRefund(transaction, route, i);
                throw e;
            }
        }
        
        // 完成交易
        transactionService.completeTransaction(
                transaction.getTransactionId(),
                route.getEstimatedAmountOut().toString()
        );
    }

    /**
     * 执行swap步骤
     */
    private String executeSwapStep(
            CrossChainRoute.RouteStep step,
            CrossChainTransaction transaction,
            double slippagePercent) throws Exception {
        
        log.info("Executing swap on chain {}: {} -> {}",
                step.getChainId(), step.getTokenIn(), step.getTokenOut());
        
        // 这里应该调用实际的swap合约
        // 为了演示，返回模拟的交易哈希
        String txHash = generateMockTxHash();
        
        log.info("Swap executed: {}", txHash);
        return txHash;
    }

    /**
     * 执行桥接步骤
     */
    private String executeBridgeStep(
            CrossChainRoute.RouteStep step,
            CrossChainTransaction transaction) throws Exception {
        
        log.info("Executing bridge from chain {} to target chain",
                step.getChainId());
        
        // 构建桥接交易
        String bridgeData = zetaChainService.buildBridgeTransaction(
                step.getChainId(),
                transaction.getTargetChainId(),
                step.getTokenIn(),
                step.getAmountIn(),
                transaction.getUserAddress()
        );
        
        // 发送跨链消息
        CompletableFuture<String> messageFuture = zetaChainService.sendCrossChainMessage(
                step.getChainId(),
                transaction.getTargetChainId(),
                bridgeData
        );
        
        String messageId = messageFuture.join(); // 等待异步结果
        
        // 更新桥接消息ID
        transactionService.updateBridgeMessageId(transaction.getTransactionId(), messageId);
        
        log.info("Bridge message sent: {}", messageId);
        return messageId;
    }

    /**
     * 等待接收
     */
    private String waitForReceive(
            CrossChainRoute.RouteStep step,
            CrossChainTransaction transaction) throws Exception {
        
        log.info("Waiting for assets to be received on chain {}", step.getChainId());
        
        // 等待桥接完成
        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            String status = zetaChainService.queryMessageStatus(
                    transaction.getBridgeMessageId()
            );
            
            if ("completed".equalsIgnoreCase(status)) {
                log.info("Assets received on target chain");
                return generateMockTxHash();
            } else if ("failed".equalsIgnoreCase(status)) {
                throw new RuntimeException("Bridge transfer failed");
            }
            
            attempts++;
            Thread.sleep(RETRY_DELAY_MS);
        }
        
        throw new RuntimeException("Timeout waiting for bridge completion");
    }

    /**
     * 处理失败和退款
     */
    private void handleFailureAndRefund(
            CrossChainTransaction transaction,
            CrossChainRoute route,
            int failedStepIndex) {
        
        log.warn("Handling failure at step {} for transaction {}",
                failedStepIndex, transaction.getTransactionId());
        
        try {
            // 如果失败发生在桥接后，尝试在目标链退款
            if (failedStepIndex > 0 && route.getSteps().get(failedStepIndex - 1).getType() 
                    == CrossChainRoute.StepType.BRIDGE) {
                
                log.info("Attempting refund on target chain");
                String refundTxHash = executeRefund(transaction, route.getTargetChainId());
                transactionService.refundTransaction(transaction.getTransactionId(), refundTxHash);
                
            } else {
                // 在源链退款
                log.info("Attempting refund on source chain");
                String refundTxHash = executeRefund(transaction, route.getSourceChainId());
                transactionService.refundTransaction(transaction.getTransactionId(), refundTxHash);
            }
            
        } catch (Exception e) {
            log.error("Refund failed for transaction {}: {}",
                    transaction.getTransactionId(), e.getMessage());
            
            // 标记为部分完成（桥接资产已到达但未完成swap）
            transactionService.partiallyCompleteTransaction(
                    transaction.getTransactionId(),
                    "Assets are on target chain but swap failed. Manual intervention required."
            );
        }
    }

    /**
     * 执行退款
     */
    private String executeRefund(CrossChainTransaction transaction, Integer chainId) throws Exception {
        log.info("Executing refund on chain {} for transaction {}",
                chainId, transaction.getTransactionId());
        
        // 这里应该调用实际的退款逻辑
        // 返回模拟的退款交易哈希
        return generateMockTxHash();
    }

    /**
     * 查询跨链交易状态
     */
    public CrossChainTransaction getTransactionStatus(String transactionId) {
        return transactionService.getTransaction(transactionId);
    }

    /**
     * 重试失败的交易
     */
    public void retryTransaction(String transactionId) {
        CrossChainTransaction transaction = transactionService.getTransaction(transactionId);
        
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        if (!transaction.isRetryable()) {
            throw new IllegalStateException("Transaction is not retryable");
        }
        
        log.info("Retrying transaction: {}", transactionId);
        transactionService.retryTransaction(transactionId);
        
        // 重新执行交易（简化版本）
        CompletableFuture.runAsync(() -> {
            try {
                // 从桥接步骤重试
                CompletableFuture<String> messageFuture = zetaChainService.sendCrossChainMessage(
                        transaction.getSourceChainId(),
                        transaction.getTargetChainId(),
                        "" // 需要重建桥接数据
                );
                String messageId = messageFuture.join();
                transactionService.updateBridgeMessageId(transactionId, messageId);
                
            } catch (Exception e) {
                log.error("Retry failed for transaction {}: {}",
                        transactionId, e.getMessage());
                transactionService.failTransaction(transactionId, "Retry failed: " + e.getMessage());
            }
        });
    }

    /**
     * 估算跨链gas费用
     */
    public CrossChainRoute.FeeBreakdown estimateGasFee(CrossChainRoute route) {
        return feeService.calculateFeeBreakdown(
                route.getSourceChainId(),
                route.getTargetChainId(),
                route.getAmountIn(),
                route.getSteps()
        );
    }

    /**
     * 检查断路器状态
     */
    public boolean isRouteAvailable(Integer sourceChainId, Integer targetChainId) {
        return !zetaChainService.isCircuitBreakerOpen(sourceChainId, targetChainId)
                && zetaChainService.isBridgePathSupported(sourceChainId, targetChainId);
    }

    /**
     * 获取支持的桥接路径
     */
    public List<String> getSupportedBridgePaths() {
        // 返回支持的链对
        return List.of(
                "1 -> 56",   // Ethereum -> BSC
                "1 -> 137",  // Ethereum -> Polygon
                "56 -> 137"  // BSC -> Polygon
        );
    }

    /**
     * 生成模拟交易哈希（用于测试）
     */
    private String generateMockTxHash() {
        return "0x" + java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
