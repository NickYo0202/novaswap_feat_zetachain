package com.novaswap.service;

import com.novaswap.contract.PairReadService;
import com.novaswap.model.PoolReserve;
import com.novaswap.model.RouteInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteSearchService {
    
    private final PairReadService pairReadService;
    private final FactoryService factoryService;
    
    @Value("${novaswap.contract.factory:0x5C69bEe701ef814a2B6a3EDD4B1652CB9cc5aA6f}")
    private String factoryAddress;
    
    private static final BigInteger FEE_NUMERATOR = BigInteger.valueOf(997);
    private static final BigInteger FEE_DENOMINATOR = BigInteger.valueOf(1000);
    
    /**
     * 搜索最优路由（直达 或 多跳）
     */
    public RouteInfo findBestRoute(
        String tokenIn, 
        String tokenOut, 
        BigInteger amountIn,
        double slippageTolerance,
        List<String> intermediateTokens
    ) {
        // 尝试直接路由
        RouteInfo directRoute = findDirectRoute(tokenIn, tokenOut, amountIn, slippageTolerance);
        
        // 尝试多跳路由
        List<RouteInfo> multiHopRoutes = findMultiHopRoutes(
            tokenIn, tokenOut, amountIn, slippageTolerance, intermediateTokens
        );
        
        // 选择输出最大的路由
        RouteInfo bestRoute = directRoute;
        for (RouteInfo route : multiHopRoutes) {
            if (route != null && route.getAmountOut().compareTo(bestRoute.getAmountOut()) > 0) {
                bestRoute = route;
            }
        }
        
        if (bestRoute.getAmountOut().equals(BigInteger.ZERO)) {
            throw new RuntimeException("No available route found");
        }
        
        log.info("Best route found: {} hops, output: {}", bestRoute.getHops(), bestRoute.getAmountOut());
        return bestRoute;
    }
    
    /**
     * 查找直达路由
     */
    private RouteInfo findDirectRoute(
        String tokenIn, 
        String tokenOut, 
        BigInteger amountIn,
        double slippageTolerance
    ) {
        try {
            String pairAddress = factoryService.getPairAddress(factoryAddress, tokenIn, tokenOut);
            PoolReserve reserves = getPoolReserves(pairAddress, tokenIn, tokenOut);
            
            BigInteger amountOut = calculateAmountOut(
                amountIn, 
                reserves.getReserve0(), 
                reserves.getReserve1()
            );
            
            BigDecimal priceImpact = calculatePriceImpact(
                amountIn, 
                reserves.getReserve0(), 
                reserves.getReserve1()
            );
            
            BigInteger minAmountOut = applySlippage(amountOut, slippageTolerance);
            
            List<String> path = Arrays.asList(tokenIn, tokenOut);
            BigInteger[] reserveArray = {reserves.getReserve0(), reserves.getReserve1()};
            
            return new RouteInfo(path, amountOut, minAmountOut, priceImpact, reserveArray, true, 1);
        } catch (Exception e) {
            log.warn("Direct route not available: {}", e.getMessage());
            return new RouteInfo(
                Arrays.asList(tokenIn, tokenOut), 
                BigInteger.ZERO, 
                BigInteger.ZERO, 
                BigDecimal.ZERO, 
                new BigInteger[]{BigInteger.ZERO, BigInteger.ZERO}, 
                true, 
                1
            );
        }
    }
    
    /**
     * 查找多跳路由
     */
    private List<RouteInfo> findMultiHopRoutes(
        String tokenIn,
        String tokenOut,
        BigInteger amountIn,
        double slippageTolerance,
        List<String> intermediateTokens
    ) {
        List<RouteInfo> routes = new ArrayList<>();
        
        if (intermediateTokens == null || intermediateTokens.isEmpty()) {
            return routes;
        }
        
        // 尝试通过每个中间代币的路由
        for (String intermediateToken : intermediateTokens) {
            try {
                RouteInfo route = findTwoHopRoute(
                    tokenIn, intermediateToken, tokenOut, amountIn, slippageTolerance
                );
                if (route != null && route.getAmountOut().compareTo(BigInteger.ZERO) > 0) {
                    routes.add(route);
                }
            } catch (Exception e) {
                log.debug("Two-hop route via {} failed: {}", intermediateToken, e.getMessage());
            }
        }
        
        return routes;
    }
    
    /**
     * 查找两跳路由
     */
    private RouteInfo findTwoHopRoute(
        String tokenIn,
        String tokenIntermediate,
        String tokenOut,
        BigInteger amountIn,
        double slippageTolerance
    ) {
        // 第一跳
        String pair1 = factoryService.getPairAddress(factoryAddress, tokenIn, tokenIntermediate);
        PoolReserve reserves1 = getPoolReserves(pair1, tokenIn, tokenIntermediate);
        BigInteger amountIntermediate = calculateAmountOut(
            amountIn, reserves1.getReserve0(), reserves1.getReserve1()
        );
        
        // 第二跳
        String pair2 = factoryService.getPairAddress(factoryAddress, tokenIntermediate, tokenOut);
        PoolReserve reserves2 = getPoolReserves(pair2, tokenIntermediate, tokenOut);
        BigInteger amountOut = calculateAmountOut(
            amountIntermediate, reserves2.getReserve0(), reserves2.getReserve1()
        );
        
        // 计算总体价格影响
        BigDecimal priceImpact1 = calculatePriceImpact(
            amountIn, reserves1.getReserve0(), reserves1.getReserve1()
        );
        BigDecimal priceImpact2 = calculatePriceImpact(
            amountIntermediate, reserves2.getReserve0(), reserves2.getReserve1()
        );
        BigDecimal totalPriceImpact = priceImpact1.add(priceImpact2);
        
        BigInteger minAmountOut = applySlippage(amountOut, slippageTolerance);
        
        List<String> path = Arrays.asList(tokenIn, tokenIntermediate, tokenOut);
        BigInteger[] reserveArray = {
            reserves1.getReserve0(), reserves1.getReserve1(),
            reserves2.getReserve0(), reserves2.getReserve1()
        };
        
        return new RouteInfo(path, amountOut, minAmountOut, totalPriceImpact, reserveArray, false, 2);
    }
    
    /**
     * 计算输出数量（基于恒定乘积公式 x*y=k）
     * amountOut = (amountIn * 997 * reserveOut) / (reserveIn * 1000 + amountIn * 997)
     */
    private BigInteger calculateAmountOut(
        BigInteger amountIn,
        BigInteger reserveIn,
        BigInteger reserveOut
    ) {
        if (amountIn.equals(BigInteger.ZERO)) {
            return BigInteger.ZERO;
        }
        
        if (reserveIn.equals(BigInteger.ZERO) || reserveOut.equals(BigInteger.ZERO)) {
            throw new RuntimeException("Insufficient liquidity");
        }
        
        BigInteger amountInWithFee = amountIn.multiply(FEE_NUMERATOR);
        BigInteger numerator = amountInWithFee.multiply(reserveOut);
        BigInteger denominator = reserveIn.multiply(FEE_DENOMINATOR).add(amountInWithFee);
        
        return numerator.divide(denominator);
    }
    
    /**
     * 计算价格影响
     */
    private BigDecimal calculatePriceImpact(
        BigInteger amountIn,
        BigInteger reserveIn,
        BigInteger reserveOut
    ) {
        if (reserveIn.equals(BigInteger.ZERO) || reserveOut.equals(BigInteger.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        // 交易前价格
        BigDecimal priceBefore = new BigDecimal(reserveOut).divide(
            new BigDecimal(reserveIn), 18, RoundingMode.HALF_UP
        );
        
        // 交易后储备
        BigInteger newReserveIn = reserveIn.add(amountIn);
        BigInteger amountOut = calculateAmountOut(amountIn, reserveIn, reserveOut);
        BigInteger newReserveOut = reserveOut.subtract(amountOut);
        
        // 交易后价格
        BigDecimal priceAfter = new BigDecimal(newReserveOut).divide(
            new BigDecimal(newReserveIn), 18, RoundingMode.HALF_UP
        );
        
        // 价格影响 = (priceAfter - priceBefore) / priceBefore * 100
        return priceAfter.subtract(priceBefore)
            .divide(priceBefore, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .abs();
    }
    
    /**
     * 应用滑点容忍度
     */
    private BigInteger applySlippage(BigInteger amount, double slippageTolerance) {
        BigDecimal slippageMultiplier = BigDecimal.ONE.subtract(
            BigDecimal.valueOf(slippageTolerance)
        );
        
        return new BigDecimal(amount)
            .multiply(slippageMultiplier)
            .toBigInteger();
    }
    
    /**
     * 获取池储备
     */
    private PoolReserve getPoolReserves(String pairAddress, String token0, String token1) {
        PairReadService.Reserves reserves = pairReadService.getReserves(pairAddress);
        
        return new PoolReserve(
            pairAddress,
            token0,
            token1,
            reserves.reserve0(),
            reserves.reserve1(),
            BigInteger.ZERO // totalSupply可以单独查询
        );
    }
}
