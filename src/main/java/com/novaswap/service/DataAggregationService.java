package com.novaswap.service;

import com.novaswap.model.PoolStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据聚合服务 - 计算TVL、APY、交易量等统计数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataAggregationService {
    
    private final EventListenerService eventListenerService;
    
    // 内存存储（实际生产环境应使用数据库）
    private final Map<String, PoolStats> poolStatsCache = new ConcurrentHashMap<>();
    
    /**
     * 计算池的TVL (Total Value Locked)
     * TVL = (reserve0 * price0 + reserve1 * price1)
     */
    public BigDecimal calculateTVL(
        BigInteger reserve0,
        BigInteger reserve1,
        BigDecimal price0Usd,
        BigDecimal price1Usd,
        int decimals0,
        int decimals1
    ) {
        BigDecimal reserve0Decimal = new BigDecimal(reserve0)
            .divide(BigDecimal.TEN.pow(decimals0), 18, RoundingMode.HALF_UP);
        BigDecimal reserve1Decimal = new BigDecimal(reserve1)
            .divide(BigDecimal.TEN.pow(decimals1), 18, RoundingMode.HALF_UP);
        
        BigDecimal value0 = reserve0Decimal.multiply(price0Usd);
        BigDecimal value1 = reserve1Decimal.multiply(price1Usd);
        
        return value0.add(value1);
    }
    
    /**
     * 计算APY
     * APY = (fee24h * 365 / TVL) * 100%
     */
    public BigDecimal calculateAPY(BigDecimal fees24hUsd, BigDecimal tvlUsd) {
        if (tvlUsd.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        // 年化手续费
        BigDecimal annualFees = fees24hUsd.multiply(BigDecimal.valueOf(365));
        
        // APY = (年化手续费 / TVL) * 100
        return annualFees.divide(tvlUsd, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * 计算24h交易量
     */
    public BigDecimal calculate24hVolume(String pairAddress) {
        // 实际应从事件中计算
        // 这里返回模拟数据
        return BigDecimal.valueOf(100000);
    }
    
    /**
     * 计算24h手续费
     * 手续费 = 交易量 * 0.3%
     */
    public BigDecimal calculate24hFees(BigDecimal volume24h) {
        return volume24h.multiply(BigDecimal.valueOf(0.003));
    }
    
    /**
     * 获取池统计信息
     */
    @Cacheable(value = "poolStats", key = "#pairAddress", unless = "#result == null")
    public PoolStats getPoolStats(String pairAddress) {
        PoolStats cached = poolStatsCache.get(pairAddress);
        if (cached != null) {
            log.debug("Returning cached pool stats for {}", pairAddress);
            return cached;
        }
        
        // 实际应从数据库或链上查询
        PoolStats stats = PoolStats.builder()
            .pairAddress(pairAddress)
            .tvlUsd(BigDecimal.valueOf(1000000))
            .volume24hUsd(BigDecimal.valueOf(100000))
            .fees24hUsd(BigDecimal.valueOf(300))
            .apy(BigDecimal.valueOf(109.5)) // (300 * 365 / 1000000) * 100
            .txCount24h(150L)
            .lpCount(50L)
            .build();
        
        poolStatsCache.put(pairAddress, stats);
        return stats;
    }
    
    /**
     * 获取热门池列表（按TVL排序）
     */
    public List<PoolStats> getTopPoolsByTVL(int limit) {
        return poolStatsCache.values().stream()
            .sorted(Comparator.comparing(PoolStats::getTvlUsd).reversed())
            .limit(limit)
            .toList();
    }
    
    /**
     * 获取热门池列表（按24h交易量排序）
     */
    public List<PoolStats> getTopPoolsByVolume(int limit) {
        return poolStatsCache.values().stream()
            .sorted(Comparator.comparing(PoolStats::getVolume24hUsd).reversed())
            .limit(limit)
            .toList();
    }
    
    /**
     * 获取所有池统计
     */
    public List<PoolStats> getAllPoolStats() {
        return new ArrayList<>(poolStatsCache.values());
    }
    
    /**
     * 更新池统计（由定时任务调用）
     */
    public void updatePoolStats(String pairAddress) {
        try {
            // 这里应该：
            // 1. 获取最新的储备数据
            // 2. 计算24h交易量
            // 3. 计算TVL
            // 4. 计算APY
            // 5. 更新缓存
            
            log.info("Pool stats updated for {}", pairAddress);
        } catch (Exception e) {
            log.error("Failed to update pool stats for {}", pairAddress, e);
        }
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        poolStatsCache.clear();
        log.info("Pool stats cache cleared");
    }
}
