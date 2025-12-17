package com.novaswap.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigInteger;
import java.time.Instant;

/**
 * Swap事件模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwapEvent {
    private String txHash;
    private String pairAddress;
    private String sender;
    private String to;
    private BigInteger amount0In;
    private BigInteger amount1In;
    private BigInteger amount0Out;
    private BigInteger amount1Out;
    private BigInteger blockNumber;
    private Instant timestamp;
    private Long chainId;
}
