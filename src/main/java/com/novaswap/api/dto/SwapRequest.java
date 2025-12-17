package com.novaswap.api.dto;

import java.math.BigInteger;
import java.util.List;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SwapRequest {
    @NotNull @Min(1)
    private BigInteger amountIn;

    @NotNull @Min(0)
    private BigInteger amountOutMin;

    @NotEmpty
    private List<@NotBlank String> path;

    @NotBlank
    private String to;

    // 交易截止时间（秒级 Unix 时间戳）；如果为空，后端可默认 20 分钟
    private Long deadline;
}
