package com.novaswap.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoolReserve {
    private String pairAddress;
    private String token0;
    private String token1;
    private BigInteger reserve0;
    private BigInteger reserve1;
    private BigInteger totalSupply;
}
