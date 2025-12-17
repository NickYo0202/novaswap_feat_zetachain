package com.novaswap.api.dto.crosschain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 跨链swap执行响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossChainSwapResponse {

    private boolean success;
    private String message;
    private String transactionId;
    private String estimatedTimeSeconds;
}
