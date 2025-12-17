package com.novaswap.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigInteger;
import java.time.Instant;

/**
 * Mint事件模型（添加流动性）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MintEvent {
    private String txHash;
    private String pairAddress;
    private String sender;
    private BigInteger amount0;
    private BigInteger amount1;
    private BigInteger liquidity;
    private BigInteger blockNumber;
    private Instant timestamp;
    private Long chainId;
}
