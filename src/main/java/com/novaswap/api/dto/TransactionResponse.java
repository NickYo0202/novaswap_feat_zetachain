package com.novaswap.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

/**
 * 交易构建响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "交易构建响应")
public class TransactionResponse {
    
    @Schema(description = "目标合约地址（Router地址）", 
            example = "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D")
    private String to;
    
    @Schema(description = "交易calldata（编码后的函数调用）", 
            example = "0x38ed1739...")
    private String data;
    
    @Schema(description = "交易value（ETH数量，Wei单位），非ETH交易为0", 
            example = "1000000000000000000")
    private String value;
    
    @Schema(description = "Gas估算（可选）", example = "150000")
    private BigInteger gasEstimate;
    
    @Schema(description = "函数名称", example = "swapExactTokensForTokens")
    private String functionName;
    
    @Schema(description = "截止时间戳", example = "1702800000")
    private BigInteger deadline;
    
    @Schema(description = "交易描述", 
            example = "Swap 1000 USDC for minimum 0.5 ETH")
    private String description;
}
