package com.novaswap.api.dto.crosschain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 支持的桥接路径响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportedBridgePathsResponse {

    private boolean success;
    private String message;
    
    private List<BridgePath> paths;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BridgePath {
        private Integer sourceChainId;
        private String sourceChainName;
        private Integer targetChainId;
        private String targetChainName;
        private Boolean available;
        private String reason;
        private Long estimatedTimeSeconds;
        private String baseFee;
    }
}
