package com.novaswap.api.dto.crosschain;

import com.novaswap.model.crosschain.CrossChainRoute;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

/**
 * 跨链路由搜索请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossChainRouteRequest {

    @NotNull(message = "Source chain ID is required")
    private Integer sourceChainId;

    @NotNull(message = "Target chain ID is required")
    private Integer targetChainId;

    @NotBlank(message = "Source token address is required")
    private String sourceTokenAddress;

    @NotBlank(message = "Target token address is required")
    private String targetTokenAddress;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigInteger amountIn;

    @Builder.Default
    private CrossChainRoute.RouteType routeType = CrossChainRoute.RouteType.BALANCED;

    @Builder.Default
    private Double slippagePercent = 0.5;
}
