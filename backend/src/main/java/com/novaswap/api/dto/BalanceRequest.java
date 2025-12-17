package com.novaswap.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BalanceRequest {
    @NotBlank(message = "User address is required")
    private String userAddress;
    
    @NotNull(message = "Token addresses are required")
    private List<String> tokenAddresses;
    
    private Long chainId;
}
