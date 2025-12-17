package com.novaswap.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

/**
 * 添加流动性交易构建请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "添加流动性交易构建请求")
public class AddLiquidityTransactionRequest {
    
    @NotBlank(message = "代币A地址不能为空")
    @Schema(description = "代币A地址", example = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48")
    private String tokenA;
    
    @NotBlank(message = "代币B地址不能为空")
    @Schema(description = "代币B地址（ETH使用0x0000000000000000000000000000000000000000）", 
            example = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2")
    private String tokenB;
    
    @NotNull(message = "代币A数量不能为空")
    @Schema(description = "代币A期望数量（Wei单位）", example = "1000000")
    private BigInteger amountADesired;
    
    @NotNull(message = "代币B数量不能为空")
    @Schema(description = "代币B期望数量（Wei单位）", example = "500000000000000000")
    private BigInteger amountBDesired;
    
    @Schema(description = "代币A最小数量（Wei单位），不设置则自动计算")
    private BigInteger amountAMin;
    
    @Schema(description = "代币B最小数量（Wei单位），不设置则自动计算")
    private BigInteger amountBMin;
    
    @Schema(description = "滑点百分比（例如：0.5表示0.5%），默认0.5%", example = "0.5")
    private Double slippagePercent = 0.5;
    
    @NotBlank(message = "接收地址不能为空")
    @Schema(description = "LP代币接收地址", example = "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb")
    private String recipient;
    
    @Schema(description = "截止时间（分钟），默认20分钟", example = "20")
    private Integer deadlineMinutes = 20;
}
