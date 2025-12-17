package com.novaswap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 价格点数据（用于K线图）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricePoint {
    private String pairAddress;
    private Instant timestamp;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    private String interval; // 1m, 5m, 15m, 1h, 4h, 1d, 1w
}
