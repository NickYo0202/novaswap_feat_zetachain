package com.novaswap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Multicall调用请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MulticallRequest {
    
    /**
     * 目标合约地址
     */
    private String target;
    
    /**
     * 调用数据（encoded function call）
     */
    private byte[] callData;
    
    /**
     * 是否允许失败（如果为true，即使调用失败也继续执行）
     */
    @Builder.Default
    private boolean allowFailure = false;
}
