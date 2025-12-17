package com.novaswap.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigInteger;
import java.util.List;

@Data
public class RouteSearchRequest {
    @NotBlank(message = "Token in address is required")
    private String tokenIn;
    
    @NotBlank(message = "Token out address is required")
    private String tokenOut;
    
    @NotNull(message = "Amount in is required")
    private BigInteger amountIn;
    
    private Double slippageTolerance = 0.005; // 默认0.5%
    
    private List<String> intermediateTokens;
}
