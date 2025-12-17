package com.novaswap.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteInfo {
    private List<String> path;
    private BigInteger amountOut;
    private BigInteger minAmountOut;
    private BigDecimal priceImpact;
    private BigInteger[] reserves;
    private boolean isDirect;
    private int hops;
}
