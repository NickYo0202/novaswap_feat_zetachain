package com.novaswap.api.dto.crosschain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 跨链swap执行请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossChainSwapRequest {

    @NotNull(message = "Source chain ID is required")
    private Integer sourceChainId;

    @NotNull(message = "Target chain ID is required")
    private Integer targetChainId;

    @NotBlank(message = "Source token address is required")
    private String sourceTokenAddress;

    @NotBlank(message = "Target token address is required")
    private String targetTokenAddress;

    @NotBlank(message = "Amount is required")
    private String amountIn;

    @NotBlank(message = "User address is required")
    private String userAddress;

    @Builder.Default
    private Double slippagePercent = 0.5;

    // 可选：指定路由步骤
    private String routeData;
}
