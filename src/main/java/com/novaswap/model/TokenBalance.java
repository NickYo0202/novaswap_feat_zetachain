package com.novaswap.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenBalance {
    private String tokenAddress;
    private String tokenSymbol;
    private String tokenName;
    private Integer decimals;
    private BigInteger balance;
    private String formattedBalance;
}
