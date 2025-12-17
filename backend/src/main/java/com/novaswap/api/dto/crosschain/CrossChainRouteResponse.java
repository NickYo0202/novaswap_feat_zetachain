package com.novaswap.api.dto.crosschain;

import com.novaswap.model.crosschain.CrossChainRoute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 跨链路由搜索响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossChainRouteResponse {

    private boolean success;
    private String message;
    private List<CrossChainRoute> routes;
    private Integer routeCount;
}
