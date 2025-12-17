package com.novaswap.api.dto.crosschain;

import com.novaswap.model.crosschain.CrossChainTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 跨链交易状态响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossChainTransactionStatusResponse {

    private boolean success;
    private String message;
    
    private String transactionId;
    private CrossChainTransaction.TransactionStatus status;
    
    private Integer sourceChainId;
    private Integer targetChainId;
    
    private String sourceTokenAddress;
    private String targetTokenAddress;
    
    private String amountIn;
    private String amountOut;
    
    private String sourceTxHash;
    private String targetTxHash;
    private String bridgeMessageId;
    
    private String sourceExplorerLink;
    private String targetExplorerLink;
    
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    
    private Long estimatedTimeSeconds;
    private Long actualTimeSeconds;
    
    private Integer retryCount;
    private Boolean retryable;
    
    private String errorMessage;
    
    private List<StatusHistoryDto> statusHistory;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistoryDto {
        private CrossChainTransaction.TransactionStatus status;
        private LocalDateTime timestamp;
        private String description;
    }
}
