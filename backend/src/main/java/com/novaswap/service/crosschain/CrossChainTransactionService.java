package com.novaswap.service.crosschain;

import com.novaswap.model.crosschain.CrossChainTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 跨链交易状态追踪服务
 * 监控交易在各个阶段的状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrossChainTransactionService {

    private final ZetaChainService zetaChainService;
    
    // 交易状态存储（生产环境应使用数据库）
    private final Map<String, CrossChainTransaction> transactions = new ConcurrentHashMap<>();
    
    // 区块浏览器URL模板
    private static final Map<Integer, String> EXPLORER_URLS = Map.of(
            1, "https://etherscan.io/tx/%s",
            56, "https://bscscan.com/tx/%s",
            137, "https://polygonscan.com/tx/%s",
            42161, "https://arbiscan.io/tx/%s",
            10, "https://optimistic.etherscan.io/tx/%s"
    );

    /**
     * 创建新的跨链交易记录
     */
    public CrossChainTransaction createTransaction(
            Integer sourceChainId,
            Integer targetChainId,
            String sourceToken,
            String targetToken,
            String amountIn,
            String userAddress) {
        
        String transactionId = generateTransactionId();
        
        CrossChainTransaction transaction = CrossChainTransaction.builder()
                .transactionId(transactionId)
                .status(CrossChainTransaction.TransactionStatus.PENDING_SOURCE_CONFIRMATION)
                .sourceChainId(sourceChainId)
                .targetChainId(targetChainId)
                .sourceTokenAddress(sourceToken)
                .targetTokenAddress(targetToken)
                .amountIn(amountIn)
                .userAddress(userAddress)
                .createdAt(LocalDateTime.now())
                .statusHistory(new ArrayList<>())
                .build();
        
        // 添加初始状态记录
        addStatusHistory(transaction, CrossChainTransaction.TransactionStatus.PENDING_SOURCE_CONFIRMATION,
                "Transaction created, waiting for source chain confirmation");
        
        transactions.put(transactionId, transaction);
        
        log.info("Created cross-chain transaction: {}", transactionId);
        return transaction;
    }

    /**
     * 更新源链交易哈希
     */
    public void updateSourceTxHash(String transactionId, String txHash) {
        CrossChainTransaction transaction = transactions.get(transactionId);
        if (transaction == null) {
            log.warn("Transaction not found: {}", transactionId);
            return;
        }
        
        transaction.setSourceTxHash(txHash);
        transaction.setStatus(CrossChainTransaction.TransactionStatus.SOURCE_CONFIRMED);
        addStatusHistory(transaction, CrossChainTransaction.TransactionStatus.SOURCE_CONFIRMED,
                "Source chain transaction confirmed: " + getExplorerLink(transaction.getSourceChainId(), txHash));
        
        log.info("Updated source tx hash for {}: {}", transactionId, txHash);
    }

    /**
     * 更新桥接消息ID
     */
    public void updateBridgeMessageId(String transactionId, String messageId) {
        CrossChainTransaction transaction = transactions.get(transactionId);
        if (transaction == null) {
            log.warn("Transaction not found: {}", transactionId);
            return;
        }
        
        transaction.setBridgeMessageId(messageId);
        transaction.setStatus(CrossChainTransaction.TransactionStatus.BRIDGE_IN_PROGRESS);
        addStatusHistory(transaction, CrossChainTransaction.TransactionStatus.BRIDGE_IN_PROGRESS,
                "Bridge message created: " + messageId);
        
        log.info("Updated bridge message ID for {}: {}", transactionId, messageId);
    }

    /**
     * 更新目标链交易哈希
     */
    public void updateTargetTxHash(String transactionId, String txHash) {
        CrossChainTransaction transaction = transactions.get(transactionId);
        if (transaction == null) {
            log.warn("Transaction not found: {}", transactionId);
            return;
        }
        
        transaction.setTargetTxHash(txHash);
        transaction.setStatus(CrossChainTransaction.TransactionStatus.TARGET_EXECUTING);
        addStatusHistory(transaction, CrossChainTransaction.TransactionStatus.TARGET_EXECUTING,
                "Target chain execution started: " + getExplorerLink(transaction.getTargetChainId(), txHash));
        
        log.info("Updated target tx hash for {}: {}", transactionId, txHash);
    }

    /**
     * 完成交易
     */
    public void completeTransaction(String transactionId, String amountOut) {
        CrossChainTransaction transaction = transactions.get(transactionId);
        if (transaction == null) {
            log.warn("Transaction not found: {}", transactionId);
            return;
        }
        
        transaction.setAmountOut(amountOut);
        transaction.setStatus(CrossChainTransaction.TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        
        // 计算实际耗时
        if (transaction.getCreatedAt() != null) {
            long actualTime = java.time.Duration.between(
                    transaction.getCreatedAt(), 
                    transaction.getCompletedAt()
            ).getSeconds();
            transaction.setActualTimeSeconds(actualTime);
        }
        
        addStatusHistory(transaction, CrossChainTransaction.TransactionStatus.COMPLETED,
                "Transaction completed successfully. Amount out: " + amountOut);
        
        log.info("Completed transaction {}: {}", transactionId, amountOut);
    }

    /**
     * 部分完成交易（桥接资产到达但未完成最终swap）
     */
    public void partiallyCompleteTransaction(String transactionId, String reason) {
        CrossChainTransaction transaction = transactions.get(transactionId);
        if (transaction == null) {
            log.warn("Transaction not found: {}", transactionId);
            return;
        }
        
        transaction.setStatus(CrossChainTransaction.TransactionStatus.PARTIALLY_COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        addStatusHistory(transaction, CrossChainTransaction.TransactionStatus.PARTIALLY_COMPLETED,
                "Transaction partially completed: " + reason);
        
        log.warn("Partially completed transaction {}: {}", transactionId, reason);
    }

    /**
     * 交易失败
     */
    public void failTransaction(String transactionId, String errorMessage) {
        CrossChainTransaction transaction = transactions.get(transactionId);
        if (transaction == null) {
            log.warn("Transaction not found: {}", transactionId);
            return;
        }
        
        transaction.setStatus(CrossChainTransaction.TransactionStatus.FAILED);
        transaction.setErrorMessage(errorMessage);
        transaction.setCompletedAt(LocalDateTime.now());
        addStatusHistory(transaction, CrossChainTransaction.TransactionStatus.FAILED,
                "Transaction failed: " + errorMessage);
        
        log.error("Failed transaction {}: {}", transactionId, errorMessage);
    }

    /**
     * 退款交易
     */
    public void refundTransaction(String transactionId, String refundTxHash) {
        CrossChainTransaction transaction = transactions.get(transactionId);
        if (transaction == null) {
            log.warn("Transaction not found: {}", transactionId);
            return;
        }
        
        transaction.setStatus(CrossChainTransaction.TransactionStatus.REFUNDED);
        transaction.setCompletedAt(LocalDateTime.now());
        addStatusHistory(transaction, CrossChainTransaction.TransactionStatus.REFUNDED,
                "Transaction refunded: " + getExplorerLink(transaction.getSourceChainId(), refundTxHash));
        
        log.info("Refunded transaction {}: {}", transactionId, refundTxHash);
    }

    /**
     * 重试交易
     */
    public void retryTransaction(String transactionId) {
        CrossChainTransaction transaction = transactions.get(transactionId);
        if (transaction == null) {
            log.warn("Transaction not found: {}", transactionId);
            return;
        }
        
        if (!transaction.isRetryable()) {
            log.warn("Transaction {} is not retryable", transactionId);
            return;
        }
        
        transaction.setRetryCount(transaction.getRetryCount() + 1);
        transaction.setStatus(CrossChainTransaction.TransactionStatus.BRIDGE_IN_PROGRESS);
        addStatusHistory(transaction, CrossChainTransaction.TransactionStatus.BRIDGE_IN_PROGRESS,
                "Transaction retry #" + transaction.getRetryCount());
        
        log.info("Retrying transaction {}, attempt #{}", transactionId, transaction.getRetryCount());
    }

    /**
     * 查询交易状态
     */
    public CrossChainTransaction getTransaction(String transactionId) {
        return transactions.get(transactionId);
    }

    /**
     * 查询用户的所有交易
     */
    public List<CrossChainTransaction> getUserTransactions(String userAddress) {
        return transactions.values().stream()
                .filter(tx -> tx.getUserAddress().equalsIgnoreCase(userAddress))
                .sorted(Comparator.comparing(CrossChainTransaction::getCreatedAt).reversed())
                .toList();
    }

    /**
     * 定期检查待处理的交易状态
     */
    @Scheduled(fixedRate = 30000) // 每30秒检查一次
    public void checkPendingTransactions() {
        log.debug("Checking pending transactions...");
        
        transactions.values().stream()
                .filter(tx -> tx.getStatus() == CrossChainTransaction.TransactionStatus.BRIDGE_IN_PROGRESS)
                .forEach(this::updateBridgeStatus);
    }

    /**
     * 更新桥接状态
     */
    private void updateBridgeStatus(CrossChainTransaction transaction) {
        if (transaction.getBridgeMessageId() == null) {
            return;
        }
        
        try {
            // 查询ZetaChain桥接状态
            String status = zetaChainService.queryMessageStatus(transaction.getBridgeMessageId());
            
            if ("completed".equalsIgnoreCase(status)) {
                updateTargetTxHash(transaction.getTransactionId(), "0x" + transaction.getBridgeMessageId());
            } else if ("failed".equalsIgnoreCase(status)) {
                failTransaction(transaction.getTransactionId(), "Bridge message failed");
            }
        } catch (Exception e) {
            log.error("Error checking bridge status for {}: {}", 
                    transaction.getTransactionId(), e.getMessage());
        }
    }

    /**
     * 添加状态历史记录
     */
    private void addStatusHistory(
            CrossChainTransaction transaction,
            CrossChainTransaction.TransactionStatus status,
            String description) {
        
        CrossChainTransaction.StatusHistory history = CrossChainTransaction.StatusHistory.builder()
                .status(status)
                .timestamp(LocalDateTime.now())
                .description(description)
                .build();
        
        transaction.getStatusHistory().add(history);
    }

    /**
     * 生成交易ID
     */
    private String generateTransactionId() {
        return "TX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * 获取区块浏览器链接
     */
    private String getExplorerLink(Integer chainId, String txHash) {
        String template = EXPLORER_URLS.getOrDefault(chainId, "https://etherscan.io/tx/%s");
        return String.format(template, txHash);
    }

    /**
     * 清理过期交易（保留30天）
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupOldTransactions() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        
        List<String> expiredTxIds = transactions.entrySet().stream()
                .filter(entry -> entry.getValue().getCreatedAt().isBefore(cutoffDate))
                .filter(entry -> isTerminalStatus(entry.getValue().getStatus()))
                .map(Map.Entry::getKey)
                .toList();
        
        expiredTxIds.forEach(transactions::remove);
        
        log.info("Cleaned up {} expired transactions", expiredTxIds.size());
    }

    /**
     * 判断是否为终止状态
     */
    private boolean isTerminalStatus(CrossChainTransaction.TransactionStatus status) {
        return status == CrossChainTransaction.TransactionStatus.COMPLETED
                || status == CrossChainTransaction.TransactionStatus.PARTIALLY_COMPLETED
                || status == CrossChainTransaction.TransactionStatus.FAILED
                || status == CrossChainTransaction.TransactionStatus.REFUNDED;
    }
}
