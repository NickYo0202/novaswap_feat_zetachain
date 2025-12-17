package com.novaswap.api.dto;

import java.math.BigInteger;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApproveRequest {
    @NotBlank
    private String token;
    @NotBlank
    private String spender;
    @NotNull @Min(0)
    private BigInteger amount;
}
