package com.novaswap.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigInteger;
import java.time.Instant;

/**
 * Burn事件模型（移除流动性）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BurnEvent {
    private String txHash;
    private String pairAddress;
    private String sender;
    private String to;
    private BigInteger amount0;
    private BigInteger amount1;
    private BigInteger liquidity;
    private BigInteger blockNumber;
    private Instant timestamp;
    private Long chainId;
}
