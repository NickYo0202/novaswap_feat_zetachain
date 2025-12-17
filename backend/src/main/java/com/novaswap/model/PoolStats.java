package com.novaswap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 池统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolStats {
    private String pairAddress;
    private String token0;
    private String token1;
    private String token0Symbol;
    private String token1Symbol;
    
    // 储备
    private BigInteger reserve0;
    private BigInteger reserve1;
    
    // TVL
    private BigDecimal tvlUsd;
    
    // 24h交易量
    private BigDecimal volume24hUsd;
    private BigInteger volume24hToken0;
    private BigInteger volume24hToken1;
    
    // 24h手续费
    private BigDecimal fees24hUsd;
    
    // APY
    private BigDecimal apy;
    
    // 交易计数
    private Long txCount24h;
    
    // 流动性提供者数量
    private Long lpCount;
    
    private Long chainId;
}
