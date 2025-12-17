package com.novaswap.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * 缓存配置
 * 使用内存缓存，实际生产环境可替换为Redis
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("poolStats"),
            new ConcurrentMapCache("tokenBalances"),
            new ConcurrentMapCache("priceHistory"),
            new ConcurrentMapCache("routeCache")
        ));
        return cacheManager;
    }
}
