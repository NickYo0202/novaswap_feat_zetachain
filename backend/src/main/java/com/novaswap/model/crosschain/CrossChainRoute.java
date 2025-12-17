package com.novaswap.model.crosschain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * 跨链路由
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossChainRoute {
    
    /**
     * 源链ID
     */
    private Integer sourceChainId;
    
    /**
     * 目标链ID
     */
    private Integer targetChainId;
    
    /**
     * 源代币地址
     */
    private String sourceTokenAddress;
    
    /**
     * 目标代币地址
     */
    private String targetTokenAddress;
    
    /**
     * 输入数量
     */
    private BigInteger amountIn;
    
    /**
     * 预估输出数量
     */
    private BigInteger estimatedAmountOut;
    
    /**
     * 最小输出数量（滑点保护）
     */
    private BigInteger minAmountOut;
    
    /**
     * 路径步骤
     */
    private List<RouteStep> steps;
    
    /**
     * 总费用明细
     */
    private FeeBreakdown feeBreakdown;
    
    /**
     * 预估完成时间（秒）
     */
    private Long estimatedTimeSeconds;
    
    /**
     * 路由类型（FASTEST, CHEAPEST, BALANCED）
     */
    private RouteType routeType;
    
    /**
     * 价格影响百分比
     */
    private BigDecimal priceImpactPercent;
    
    /**
     * 路由步骤
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteStep {
        /**
         * 步骤类型（SWAP, BRIDGE）
         */
        private StepType type;
        
        /**
         * 链ID
         */
        private Integer chainId;
        
        /**
         * 协议名称（Uniswap, ZetaChain等）
         */
        private String protocol;
        
        /**
         * 输入代币
         */
        private String tokenIn;
        
        /**
         * 输出代币
         */
        private String tokenOut;
        
        /**
         * 输入数量
         */
        private BigInteger amountIn;
        
        /**
         * 输出数量
         */
        private BigInteger amountOut;
        
        /**
         * 手续费（0.3% for swap）
         */
        private BigInteger fee;
        
        /**
         * 步骤描述
         */
        private String description;
    }
    
    /**
     * 费用分解
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeBreakdown {
        /**
         * 源链Gas费用
         */
        private BigInteger sourceChainGasFee;
        
        /**
         * 桥接费用
         */
        private BigInteger bridgeFee;
        
        /**
         * 目标链Gas费用
         */
        private BigInteger targetChainGasFee;
        
        /**
         * 跨链服务费（0.05%）
         */
        private BigInteger serviceFee;
        
        /**
         * 第三方费用
         */
        private BigInteger thirdPartyFee;
        
        /**
         * 总费用（所有费用之和）
         */
        private BigInteger totalFee;
        
        /**
         * 费用币种（USD等）
         */
        private String feeCurrency;
        
        /**
         * 费用USD价值
         */
        private BigDecimal feeInUsd;
    }
    
    /**
     * 路由类型
     */
    public enum RouteType {
        FASTEST,    // 最快路径
        CHEAPEST,   // 最省钱路径
        BALANCED    // 平衡路径
    }
    
    /**
     * 步骤类型
     */
    public enum StepType {
        SWAP,       // 兑换
        BRIDGE,     // 桥接
        RECEIVE     // 接收
    }
}
