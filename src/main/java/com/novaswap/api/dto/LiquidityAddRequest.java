package com.novaswap.api.dto;

import java.math.BigInteger;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LiquidityAddRequest {
    @NotBlank
    private String tokenA;
    @NotBlank
    private String tokenB;
    @NotNull @Min(1)
    private BigInteger amountADesired;
    @NotNull @Min(1)
    private BigInteger amountBDesired;
    @NotNull @Min(0)
    private BigInteger amountAMin;
    @NotNull @Min(0)
    private BigInteger amountBMin;
    @NotBlank
    private String to;
    private Long deadline;
}
