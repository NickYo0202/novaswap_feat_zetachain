package com.novaswap.model.crosschain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;import java.time.Instant;import java.util.List;
import java.util.Map;

/**
 * 桥接配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BridgeConfig {
    
    /**
     * 桥接名称（ZetaChain等）
     */
    private String bridgeName;
    
    /**
     * 支持的链对
     */
    private List<ChainPair> supportedChainPairs;
    
    /**
     * 桥接资产配置
     */
    private Map<Integer, List<String>> bridgeAssets;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 断路器状态
     */
    private CircuitBreakerStatus circuitBreaker;
    
    /**
     * 每日流出限额
     */
    private Map<Integer, BigInteger> dailyOutflowLimit;
    
    /**
     * 当前每日流出量
     */
    private Map<Integer, BigInteger> currentDailyOutflow;
    
    /**
     * 最小桥接金额
     */
    private BigInteger minBridgeAmount;
    
    /**
     * 最大桥接金额
     */
    private BigInteger maxBridgeAmount;
    
    /**
     * 历史平均桥接时间（秒）
     */
    private Map<String, Long> averageBridgeTime;
    
    /**
     * 链对配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChainPair {
        private Integer sourceChainId;
        private Integer targetChainId;
        private Boolean enabled;
        private BigInteger baseFee;
        private Long averageTimeSeconds;
    }
    
    /**
     * 断路器状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircuitBreakerStatus {
        private Boolean isOpen;
        private String reason;
        private Instant openedAt;
        private List<String> affectedRoutes;
    }
}
