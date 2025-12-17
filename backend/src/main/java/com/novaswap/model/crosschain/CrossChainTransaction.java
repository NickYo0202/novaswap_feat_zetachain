package com.novaswap.model.crosschain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 跨链交易记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossChainTransaction {
    
    /**
     * 交易ID
     */
    private String transactionId;
    
    /**
     * 用户地址
     */
    private String userAddress;
    
    /**
     * 源链ID
     */
    private Integer sourceChainId;
    
    /**
     * 目标链ID
     */
    private Integer targetChainId;
    
    /**
     * 源代币
     */
    private String sourceTokenAddress;
    
    /**
     * 目标代币
     */
    private String targetTokenAddress;
    
    /**
     * 输入数量
     */
    private String amountIn;
    
    /**
     * 实际输出数量
     */
    private String amountOut;
    
    /**
     * 当前状态
     */
    private TransactionStatus status;
    
    /**
     * 状态历史
     */
    private List<StatusHistory> statusHistory;
    
    /**
     * 源链交易哈希
     */
    private String sourceTxHash;
    
    /**
     * 桥接消息ID
     */
    private String bridgeMessageId;
    
    /**
     * 目标链交易哈希
     */
    private String targetTxHash;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
    
    /**
     * 预估完成时间（秒）
     */
    private Long estimatedTimeSeconds;
    
    /**
     * 实际耗时（秒）
     */
    private Long actualTimeSeconds;
    
    /**
     * 路由信息
     */
    private CrossChainRoute route;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 是否可重试
     */
    private Boolean retryable;
    
    /**
     * 重试次数
     */
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * 判断是否可重试
     */
    public boolean isRetryable() {
        return (status == TransactionStatus.FAILED || status == TransactionStatus.BRIDGE_IN_PROGRESS)
                && retryCount < 3;
    }
    
    /**
     * 交易状态
     */
    public enum TransactionStatus {
        PENDING_SOURCE_CONFIRMATION,    // 等待源链确认
        SOURCE_CONFIRMED,               // 源链已确认
        BRIDGE_INITIATED,               // 桥接已发起
        BRIDGE_IN_PROGRESS,             // 桥接进行中
        TARGET_EXECUTING,               // 目标链执行中
        COMPLETED,                      // 已完成
        PARTIALLY_COMPLETED,            // 部分完成
        FAILED,                         // 失败
        REFUNDED                        // 已退回
    }
    
    /**
     * 状态历史记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistory {
        private TransactionStatus status;
        private LocalDateTime timestamp;
        private String description;
    }
}
