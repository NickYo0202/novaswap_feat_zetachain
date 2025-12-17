package com.novaswap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Multicall调用结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MulticallResult {
    
    /**
     * 调用是否成功
     */
    private boolean success;
    
    /**
     * 返回数据
     */
    private byte[] returnData;
    
    /**
     * 错误信息（如果失败）
     */
    private String error;
}
