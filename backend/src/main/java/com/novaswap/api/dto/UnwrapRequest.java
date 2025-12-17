package com.novaswap.api.dto;

import java.math.BigInteger;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UnwrapRequest {
    @NotNull @Min(1)
    private BigInteger amountWei;
}
