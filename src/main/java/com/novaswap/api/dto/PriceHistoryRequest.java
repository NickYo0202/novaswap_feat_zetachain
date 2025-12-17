package com.novaswap.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistoryRequest {
    private String pairAddress;
    private String interval; // 1m, 5m, 15m, 1h, 4h, 1d, 1w
    private Long startTime; // Unix timestamp
    private Long endTime; // Unix timestamp
    private Integer limit; // 最大返回数量
}
