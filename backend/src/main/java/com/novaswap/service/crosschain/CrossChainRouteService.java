package com.novaswap.service.crosschain;

import com.novaswap.model.crosschain.CrossChainRoute;
import com.novaswap.service.RouteSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

/**
 * 跨链路径搜索服务
 * 综合输出、手续费、时间选择最优路径
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrossChainRouteService {

    private final ZetaChainService zetaChainService;
    private final RouteSearchService routeSearchService;
    private final CrossChainFeeService feeService;
    
    // 稳定币地址配置（用于中转）
    private static final Map<Integer, String> USDC_ADDRESSES = Map.of(
            1, "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
            56, "0x8AC76a51cc950d9822D68b83fE1Ad97B32Cd580d",
            137, "0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174"
    );
    
    private static final BigDecimal SWAP_FEE_PERCENT = new BigDecimal("0.003"); // 0.3%

    /**
     * 搜索跨链路由
     * @param sourceChainId 源链ID
     * @param targetChainId 目标链ID
     * @param sourceToken 源代币地址
     * @param targetToken 目标代币地址
     * @param amountIn 输入数量
     * @param routeType 路由类型（FASTEST/CHEAPEST/BALANCED）
     * @return 跨链路由列表
     */
    public List<CrossChainRoute> searchRoutes(
            Integer sourceChainId,
            Integer targetChainId,
            String sourceToken,
            String targetToken,
            BigInteger amountIn,
            CrossChainRoute.RouteType routeType) {
        
        log.info("Searching cross-chain routes: {}->{}, token: {}->{}, amount: {}", 
                sourceChainId, targetChainId, sourceToken, targetToken, amountIn);
        
        // 检查桥接是否支持
        if (!zetaChainService.isBridgePathSupported(sourceChainId, targetChainId)) {
            log.warn("Bridge path not supported: {} -> {}", sourceChainId, targetChainId);
            return Collections.emptyList();
        }
        
        List<CrossChainRoute> routes = new ArrayList<>();
        
        // 1. 尝试直接桥接（如果源代币和目标代币是桥接资产）
        CrossChainRoute directRoute = buildDirectBridgeRoute(
                sourceChainId, targetChainId, sourceToken, targetToken, amountIn);
        if (directRoute != null) {
            routes.add(directRoute);
        }
        
        // 2. 尝试通过稳定币中转的路由
        CrossChainRoute stablecoinRoute = buildStablecoinBridgeRoute(
                sourceChainId, targetChainId, sourceToken, targetToken, amountIn);
        if (stablecoinRoute != null) {
            routes.add(stablecoinRoute);
        }
        
        // 根据路由类型排序
        routes = sortRoutesByType(routes, routeType);
        
        if (routes.isEmpty()) {
            log.error("No cross-chain route found for: {} -> {}", sourceChainId, targetChainId);
        }
        
        return routes;
    }

    /**
     * 构建直接桥接路由
     */
    private CrossChainRoute buildDirectBridgeRoute(
            Integer sourceChainId, Integer targetChainId,
            String sourceToken, String targetToken,
            BigInteger amountIn) {
        
        List<CrossChainRoute.RouteStep> steps = new ArrayList<>();
        BigInteger currentAmount = amountIn;
        
        // Step 1: 桥接
        BigInteger bridgeFee = zetaChainService.getBridgeFee(sourceChainId, targetChainId);
        BigInteger amountAfterBridge = currentAmount.subtract(bridgeFee);
        
        steps.add(CrossChainRoute.RouteStep.builder()
                .type(CrossChainRoute.StepType.BRIDGE)
                .chainId(sourceChainId)
                .protocol("ZetaChain")
                .tokenIn(sourceToken)
                .tokenOut(targetToken)
                .amountIn(currentAmount)
                .amountOut(amountAfterBridge)
                .fee(bridgeFee)
                .description(String.format("Bridge from chain %d to chain %d", sourceChainId, targetChainId))
                .build());
        
        // 计算费用分解
        CrossChainRoute.FeeBreakdown feeBreakdown = feeService.calculateFeeBreakdown(
                sourceChainId, targetChainId, amountIn, steps);
        
        // 预估完成时间
        Long estimatedTime = zetaChainService.getEstimatedBridgeTime(sourceChainId, targetChainId);
        
        return CrossChainRoute.builder()
                .sourceChainId(sourceChainId)
                .targetChainId(targetChainId)
                .sourceTokenAddress(sourceToken)
                .targetTokenAddress(targetToken)
                .amountIn(amountIn)
                .estimatedAmountOut(amountAfterBridge)
                .minAmountOut(calculateMinAmount(amountAfterBridge, 0.5))
                .steps(steps)
                .feeBreakdown(feeBreakdown)
                .estimatedTimeSeconds(estimatedTime)
                .routeType(CrossChainRoute.RouteType.FASTEST)
                .priceImpactPercent(BigDecimal.ZERO)
                .build();
    }

    /**
     * 构建通过稳定币中转的路由
     */
    private CrossChainRoute buildStablecoinBridgeRoute(
            Integer sourceChainId, Integer targetChainId,
            String sourceToken, String targetToken,
            BigInteger amountIn) {
        
        String sourceChainUsdc = USDC_ADDRESSES.get(sourceChainId);
        String targetChainUsdc = USDC_ADDRESSES.get(targetChainId);
        
        if (sourceChainUsdc == null || targetChainUsdc == null) {
            return null;
        }
        
        List<CrossChainRoute.RouteStep> steps = new ArrayList<>();
        BigInteger currentAmount = amountIn;
        
        // Step 1: 源链swap to USDC（如果不是USDC）
        if (!sourceToken.equalsIgnoreCase(sourceChainUsdc)) {
            BigInteger swapFee = currentAmount.multiply(new BigInteger("3"))
                    .divide(new BigInteger("1000")); // 0.3%
            BigInteger amountOut = currentAmount.subtract(swapFee);
            
            steps.add(CrossChainRoute.RouteStep.builder()
                    .type(CrossChainRoute.StepType.SWAP)
                    .chainId(sourceChainId)
                    .protocol("Uniswap V2")
                    .tokenIn(sourceToken)
                    .tokenOut(sourceChainUsdc)
                    .amountIn(currentAmount)
                    .amountOut(amountOut)
                    .fee(swapFee)
                    .description("Swap to USDC on source chain")
                    .build());
            
            currentAmount = amountOut;
        }
        
        // Step 2: 桥接USDC
        BigInteger bridgeFee = zetaChainService.getBridgeFee(sourceChainId, targetChainId);
        BigInteger amountAfterBridge = currentAmount.subtract(bridgeFee);
        
        steps.add(CrossChainRoute.RouteStep.builder()
                .type(CrossChainRoute.StepType.BRIDGE)
                .chainId(sourceChainId)
                .protocol("ZetaChain")
                .tokenIn(sourceChainUsdc)
                .tokenOut(targetChainUsdc)
                .amountIn(currentAmount)
                .amountOut(amountAfterBridge)
                .fee(bridgeFee)
                .description("Bridge USDC across chains")
                .build());
        
        currentAmount = amountAfterBridge;
        
        // Step 3: 目标链swap from USDC（如果目标不是USDC）
        if (!targetToken.equalsIgnoreCase(targetChainUsdc)) {
            BigInteger swapFee = currentAmount.multiply(new BigInteger("3"))
                    .divide(new BigInteger("1000")); // 0.3%
            BigInteger amountOut = currentAmount.subtract(swapFee);
            
            steps.add(CrossChainRoute.RouteStep.builder()
                    .type(CrossChainRoute.StepType.SWAP)
                    .chainId(targetChainId)
                    .protocol("Uniswap V2")
                    .tokenIn(targetChainUsdc)
                    .tokenOut(targetToken)
                    .amountIn(currentAmount)
                    .amountOut(amountOut)
                    .fee(swapFee)
                    .description("Swap from USDC on target chain")
                    .build());
            
            currentAmount = amountOut;
        }
        
        // 计算费用分解
        CrossChainRoute.FeeBreakdown feeBreakdown = feeService.calculateFeeBreakdown(
                sourceChainId, targetChainId, amountIn, steps);
        
        // 预估完成时间（桥接时间 + swap时间）
        Long estimatedTime = zetaChainService.getEstimatedBridgeTime(sourceChainId, targetChainId) + 60L;
        
        return CrossChainRoute.builder()
                .sourceChainId(sourceChainId)
                .targetChainId(targetChainId)
                .sourceTokenAddress(sourceToken)
                .targetTokenAddress(targetToken)
                .amountIn(amountIn)
                .estimatedAmountOut(currentAmount)
                .minAmountOut(calculateMinAmount(currentAmount, 0.5))
                .steps(steps)
                .feeBreakdown(feeBreakdown)
                .estimatedTimeSeconds(estimatedTime)
                .routeType(CrossChainRoute.RouteType.CHEAPEST)
                .priceImpactPercent(calculatePriceImpact(amountIn, currentAmount))
                .build();
    }

    /**
     * 根据路由类型排序
     */
    private List<CrossChainRoute> sortRoutesByType(
            List<CrossChainRoute> routes, 
            CrossChainRoute.RouteType routeType) {
        
        switch (routeType) {
            case FASTEST:
                routes.sort(Comparator.comparing(CrossChainRoute::getEstimatedTimeSeconds));
                break;
            case CHEAPEST:
                routes.sort(Comparator.comparing(r -> r.getFeeBreakdown().getTotalFee()));
                break;
            case BALANCED:
                // 综合评分：时间和费用的加权
                routes.sort((r1, r2) -> {
                    double score1 = calculateBalancedScore(r1);
                    double score2 = calculateBalancedScore(r2);
                    return Double.compare(score1, score2);
                });
                break;
        }
        
        return routes;
    }

    /**
     * 计算平衡评分
     */
    private double calculateBalancedScore(CrossChainRoute route) {
        // 归一化时间和费用，然后加权求和
        double timeWeight = 0.4;
        double feeWeight = 0.6;
        
        double normalizedTime = route.getEstimatedTimeSeconds() / 600.0; // 以10分钟为基准
        double normalizedFee = route.getFeeBreakdown().getTotalFee().doubleValue() / 10000000.0; // 归一化费用
        
        return timeWeight * normalizedTime + feeWeight * normalizedFee;
    }

    /**
     * 计算最小输出量（滑点保护）
     */
    private BigInteger calculateMinAmount(BigInteger amount, double slippagePercent) {
        BigDecimal amountDecimal = new BigDecimal(amount);
        BigDecimal multiplier = BigDecimal.ONE.subtract(
                BigDecimal.valueOf(slippagePercent).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        );
        return amountDecimal.multiply(multiplier).toBigInteger();
    }

    /**
     * 计算价格影响
     */
    private BigDecimal calculatePriceImpact(BigInteger amountIn, BigInteger amountOut) {
        if (amountIn.equals(BigInteger.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal loss = new BigDecimal(amountIn.subtract(amountOut));
        BigDecimal impact = loss.divide(new BigDecimal(amountIn), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        return impact;
    }

    /**
     * 验证路由可行性
     */
    public boolean validateRoute(CrossChainRoute route) {
        // 检查断路器
        if (zetaChainService.isCircuitBreakerOpen(
                route.getSourceChainId(), route.getTargetChainId())) {
            return false;
        }
        
        // 检查金额限制
        BigInteger minAmount = BigInteger.valueOf(1000000); // 1 USDC
        BigInteger maxAmount = new BigInteger("1000000000000000000000"); // 1000 tokens
        
        return route.getAmountIn().compareTo(minAmount) >= 0 
                && route.getAmountIn().compareTo(maxAmount) <= 0;
    }
}
