package com.novaswap.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigInteger;

@Data
public class AllowanceCheckRequest {
    @NotBlank(message = "Token address is required")
    private String tokenAddress;
    
    @NotBlank(message = "Owner address is required")
    private String owner;
    
    @NotBlank(message = "Spender address is required")
    private String spender;
    
    @NotNull(message = "Amount is required")
    private BigInteger amount;
}
