package com.novaswap.service;

import com.novaswap.model.PricePoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 价格历史服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceHistoryService {
    
    // 价格历史缓存 key: pairAddress_interval
    private final Map<String, List<PricePoint>> priceHistoryCache = new ConcurrentHashMap<>();
    
    /**
     * 获取价格历史（K线数据）
     */
    public List<PricePoint> getPriceHistory(
        String pairAddress,
        String interval,
        Instant startTime,
        Instant endTime
    ) {
        String cacheKey = pairAddress + "_" + interval;
        List<PricePoint> cached = priceHistoryCache.get(cacheKey);
        
        if (cached != null && !cached.isEmpty()) {
            // 过滤时间范围
            return cached.stream()
                .filter(p -> !p.getTimestamp().isBefore(startTime) && !p.getTimestamp().isAfter(endTime))
                .toList();
        }
        
        // 实际应从数据库查询或从事件计算
        return generateMockPriceHistory(pairAddress, interval, startTime, endTime);
    }
    
    /**
     * 获取当前价格
     */
    public BigDecimal getCurrentPrice(String pairAddress, BigInteger reserve0, BigInteger reserve1) {
        if (reserve0.equals(BigInteger.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        // 价格 = reserve1 / reserve0
        return new BigDecimal(reserve1)
            .divide(new BigDecimal(reserve0), 18, RoundingMode.HALF_UP);
    }
    
    /**
     * 添加价格点
     */
    public void addPricePoint(PricePoint pricePoint) {
        String cacheKey = pricePoint.getPairAddress() + "_" + pricePoint.getInterval();
        priceHistoryCache.computeIfAbsent(cacheKey, k -> new ArrayList<>()).add(pricePoint);
        log.debug("Added price point for {} at {}", pricePoint.getPairAddress(), pricePoint.getTimestamp());
    }
    
    /**
     * 聚合价格数据到不同时间粒度
     */
    public List<PricePoint> aggregatePriceData(
        List<PricePoint> rawData,
        String targetInterval
    ) {
        List<PricePoint> aggregated = new ArrayList<>();
        
        // 按时间间隔分组并计算OHLC
        // 实际实现应该更复杂，这里简化处理
        
        return aggregated;
    }
    
    /**
     * 计算价格变化百分比
     */
    public BigDecimal calculatePriceChange(BigDecimal currentPrice, BigDecimal previousPrice) {
        if (previousPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return currentPrice.subtract(previousPrice)
            .divide(previousPrice, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * 获取支持的时间间隔
     */
    public List<String> getSupportedIntervals() {
        return List.of("1m", "5m", "15m", "1h", "4h", "1d", "1w");
    }
    
    /**
     * 生成模拟价格历史数据（用于测试）
     */
    private List<PricePoint> generateMockPriceHistory(
        String pairAddress,
        String interval,
        Instant startTime,
        Instant endTime
    ) {
        List<PricePoint> history = new ArrayList<>();
        
        long intervalMinutes = getIntervalMinutes(interval);
        Instant current = startTime;
        BigDecimal basePrice = BigDecimal.valueOf(1000);
        
        while (current.isBefore(endTime)) {
            // 模拟价格波动
            double volatility = 0.02; // 2%波动
            BigDecimal open = basePrice;
            BigDecimal high = basePrice.multiply(BigDecimal.valueOf(1 + volatility * Math.random()));
            BigDecimal low = basePrice.multiply(BigDecimal.valueOf(1 - volatility * Math.random()));
            BigDecimal close = basePrice.multiply(BigDecimal.valueOf(1 + (volatility * 2 * Math.random() - volatility)));
            
            PricePoint point = PricePoint.builder()
                .pairAddress(pairAddress)
                .timestamp(current)
                .open(open)
                .high(high)
                .low(low)
                .close(close)
                .volume(BigDecimal.valueOf(10000 + Math.random() * 50000))
                .interval(interval)
                .build();
            
            history.add(point);
            
            basePrice = close;
            current = current.plus(intervalMinutes, ChronoUnit.MINUTES);
        }
        
        return history;
    }
    
    private long getIntervalMinutes(String interval) {
        return switch (interval) {
            case "1m" -> 1;
            case "5m" -> 5;
            case "15m" -> 15;
            case "1h" -> 60;
            case "4h" -> 240;
            case "1d" -> 1440;
            case "1w" -> 10080;
            default -> 60;
        };
    }
}
