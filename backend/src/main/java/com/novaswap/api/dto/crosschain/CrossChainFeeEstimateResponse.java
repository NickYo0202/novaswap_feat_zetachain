package com.novaswap.api.dto.crosschain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 跨链费用估算响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossChainFeeEstimateResponse {

    private boolean success;
    private String message;
    
    private BigInteger sourceChainGasFee;
    private BigInteger bridgeFee;
    private BigInteger targetChainGasFee;
    private BigInteger serviceFee;
    private BigInteger thirdPartyFee;
    private BigInteger totalFee;
    
    // USD估值
    private BigDecimal totalFeeUsd;
    
    // 费用占交易金额的百分比
    private BigDecimal feePercentage;
    
    private String feeBreakdownDescription;
}
