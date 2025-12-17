package com.novaswap.service;

import com.novaswap.config.ContractProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 交易构建服务 - 生成Router合约调用的calldata
 * 支持：swap、addLiquidity、removeLiquidity、swapETH等操作
 */
@Slf4j
@Service
public class TransactionBuilderService {

    private final ContractProperties contracts;

    public TransactionBuilderService(ContractProperties contracts) {
        this.contracts = contracts;
    }

    /**
     * 构建精确输入代币兑换交易
     * @param amountIn 输入数量
     * @param amountOutMin 最小输出数量（滑点保护）
     * @param path 兑换路径
     * @param to 接收地址
     * @param deadline 截止时间戳
     * @return calldata
     */
    public String buildSwapExactTokensForTokens(
            BigInteger amountIn,
            BigInteger amountOutMin,
            List<String> path,
            String to,
            BigInteger deadline) {
        
        log.debug("Building swapExactTokensForTokens: amountIn={}, amountOutMin={}, path={}", 
                amountIn, amountOutMin, path);

        Function function = new Function(
                "swapExactTokensForTokens",
                Arrays.asList(
                        new Uint256(amountIn),
                        new Uint256(amountOutMin),
                        new DynamicArray<>(Address.class, 
                                path.stream().map(Address::new).collect(Collectors.toList())),
                        new Address(to),
                        new Uint256(deadline)
                ),
                Collections.emptyList()
        );

        return FunctionEncoder.encode(function);
    }

    /**
     * 构建精确输出代币兑换交易
     * @param amountOut 输出数量
     * @param amountInMax 最大输入数量（滑点保护）
     * @param path 兑换路径
     * @param to 接收地址
     * @param deadline 截止时间戳
     * @return calldata
     */
    public String buildSwapTokensForExactTokens(
            BigInteger amountOut,
            BigInteger amountInMax,
            List<String> path,
            String to,
            BigInteger deadline) {
        
        log.debug("Building swapTokensForExactTokens: amountOut={}, amountInMax={}, path={}", 
                amountOut, amountInMax, path);

        Function function = new Function(
                "swapTokensForExactTokens",
                Arrays.asList(
                        new Uint256(amountOut),
                        new Uint256(amountInMax),
                        new DynamicArray<>(Address.class, 
                                path.stream().map(Address::new).collect(Collectors.toList())),
                        new Address(to),
                        new Uint256(deadline)
                ),
                Collections.emptyList()
        );

        return FunctionEncoder.encode(function);
    }

    /**
     * 构建ETH兑换代币交易（精确ETH输入）
     * @param amountOutMin 最小输出代币数量
     * @param path 兑换路径（第一个必须是WETH）
     * @param to 接收地址
     * @param deadline 截止时间戳
     * @return calldata
     */
    public String buildSwapExactETHForTokens(
            BigInteger amountOutMin,
            List<String> path,
            String to,
            BigInteger deadline) {
        
        log.debug("Building swapExactETHForTokens: amountOutMin={}, path={}", amountOutMin, path);

        Function function = new Function(
                "swapExactETHForTokens",
                Arrays.asList(
                        new Uint256(amountOutMin),
                        new DynamicArray<>(Address.class, 
                                path.stream().map(Address::new).collect(Collectors.toList())),
                        new Address(to),
                        new Uint256(deadline)
                ),
                Collections.emptyList()
        );

        return FunctionEncoder.encode(function);
    }

    /**
     * 构建代币兑换ETH交易（精确代币输入）
     * @param amountIn 输入代币数量
     * @param amountOutMin 最小输出ETH数量
     * @param path 兑换路径（最后一个必须是WETH）
     * @param to 接收地址
     * @param deadline 截止时间戳
     * @return calldata
     */
    public String buildSwapExactTokensForETH(
            BigInteger amountIn,
            BigInteger amountOutMin,
            List<String> path,
            String to,
            BigInteger deadline) {
        
        log.debug("Building swapExactTokensForETH: amountIn={}, amountOutMin={}, path={}", 
                amountIn, amountOutMin, path);

        Function function = new Function(
                "swapExactTokensForETH",
                Arrays.asList(
                        new Uint256(amountIn),
                        new Uint256(amountOutMin),
                        new DynamicArray<>(Address.class, 
                                path.stream().map(Address::new).collect(Collectors.toList())),
                        new Address(to),
                        new Uint256(deadline)
                ),
                Collections.emptyList()
        );

        return FunctionEncoder.encode(function);
    }

    /**
     * 构建添加流动性交易
     * @param tokenA 代币A地址
     * @param tokenB 代币B地址
     * @param amountADesired 期望添加的代币A数量
     * @param amountBDesired 期望添加的代币B数量
     * @param amountAMin 最小代币A数量（滑点保护）
     * @param amountBMin 最小代币B数量（滑点保护）
     * @param to LP代币接收地址
     * @param deadline 截止时间戳
     * @return calldata
     */
    public String buildAddLiquidity(
            String tokenA,
            String tokenB,
            BigInteger amountADesired,
            BigInteger amountBDesired,
            BigInteger amountAMin,
            BigInteger amountBMin,
            String to,
            BigInteger deadline) {
        
        log.debug("Building addLiquidity: tokenA={}, tokenB={}, amountA={}, amountB={}", 
                tokenA, tokenB, amountADesired, amountBDesired);

        Function function = new Function(
                "addLiquidity",
                Arrays.asList(
                        new Address(tokenA),
                        new Address(tokenB),
                        new Uint256(amountADesired),
                        new Uint256(amountBDesired),
                        new Uint256(amountAMin),
                        new Uint256(amountBMin),
                        new Address(to),
                        new Uint256(deadline)
                ),
                Collections.emptyList()
        );

        return FunctionEncoder.encode(function);
    }

    /**
     * 构建添加ETH流动性交易
     * @param token 代币地址
     * @param amountTokenDesired 期望添加的代币数量
     * @param amountTokenMin 最小代币数量
     * @param amountETHMin 最小ETH数量
     * @param to LP代币接收地址
     * @param deadline 截止时间戳
     * @return calldata
     */
    public String buildAddLiquidityETH(
            String token,
            BigInteger amountTokenDesired,
            BigInteger amountTokenMin,
            BigInteger amountETHMin,
            String to,
            BigInteger deadline) {
        
        log.debug("Building addLiquidityETH: token={}, amountToken={}, amountETHMin={}", 
                token, amountTokenDesired, amountETHMin);

        Function function = new Function(
                "addLiquidityETH",
                Arrays.asList(
                        new Address(token),
                        new Uint256(amountTokenDesired),
                        new Uint256(amountTokenMin),
                        new Uint256(amountETHMin),
                        new Address(to),
                        new Uint256(deadline)
                ),
                Collections.emptyList()
        );

        return FunctionEncoder.encode(function);
    }

    /**
     * 构建移除流动性交易
     * @param tokenA 代币A地址
     * @param tokenB 代币B地址
     * @param liquidity LP代币数量
     * @param amountAMin 最小代币A返还量
     * @param amountBMin 最小代币B返还量
     * @param to 代币接收地址
     * @param deadline 截止时间戳
     * @return calldata
     */
    public String buildRemoveLiquidity(
            String tokenA,
            String tokenB,
            BigInteger liquidity,
            BigInteger amountAMin,
            BigInteger amountBMin,
            String to,
            BigInteger deadline) {
        
        log.debug("Building removeLiquidity: tokenA={}, tokenB={}, liquidity={}", 
                tokenA, tokenB, liquidity);

        Function function = new Function(
                "removeLiquidity",
                Arrays.asList(
                        new Address(tokenA),
                        new Address(tokenB),
                        new Uint256(liquidity),
                        new Uint256(amountAMin),
                        new Uint256(amountBMin),
                        new Address(to),
                        new Uint256(deadline)
                ),
                Collections.emptyList()
        );

        return FunctionEncoder.encode(function);
    }

    /**
     * 构建移除ETH流动性交易
     * @param token 代币地址
     * @param liquidity LP代币数量
     * @param amountTokenMin 最小代币返还量
     * @param amountETHMin 最小ETH返还量
     * @param to 代币接收地址
     * @param deadline 截止时间戳
     * @return calldata
     */
    public String buildRemoveLiquidityETH(
            String token,
            BigInteger liquidity,
            BigInteger amountTokenMin,
            BigInteger amountETHMin,
            String to,
            BigInteger deadline) {
        
        log.debug("Building removeLiquidityETH: token={}, liquidity={}, amountETHMin={}", 
                token, liquidity, amountETHMin);

        Function function = new Function(
                "removeLiquidityETH",
                Arrays.asList(
                        new Address(token),
                        new Uint256(liquidity),
                        new Uint256(amountTokenMin),
                        new Uint256(amountETHMin),
                        new Address(to),
                        new Uint256(deadline)
                ),
                Collections.emptyList()
        );

        return FunctionEncoder.encode(function);
    }

    /**
     * 获取Router合约地址
     */
    public String getRouterAddress() {
        return contracts.getRouter();
    }

    /**
     * 计算deadline（当前时间 + 指定分钟数）
     * @param minutesFromNow 从现在开始的分钟数
     * @return deadline时间戳
     */
    public BigInteger calculateDeadline(int minutesFromNow) {
        long currentTime = System.currentTimeMillis() / 1000;
        long deadline = currentTime + (minutesFromNow * 60L);
        return BigInteger.valueOf(deadline);
    }

    /**
     * 计算最小输出量（基于滑点）
     * @param amount 预期数量
     * @param slippagePercent 滑点百分比（例如：0.5表示0.5%）
     * @return 最小输出量
     */
    public BigInteger calculateMinAmount(BigInteger amount, double slippagePercent) {
        double multiplier = 1.0 - (slippagePercent / 100.0);
        return amount.multiply(BigInteger.valueOf((long)(multiplier * 10000)))
                     .divide(BigInteger.valueOf(10000));
    }

    /**
     * 计算最大输入量（基于滑点）
     * @param amount 预期数量
     * @param slippagePercent 滑点百分比（例如：0.5表示0.5%）
     * @return 最大输入量
     */
    public BigInteger calculateMaxAmount(BigInteger amount, double slippagePercent) {
        double multiplier = 1.0 + (slippagePercent / 100.0);
        return amount.multiply(BigInteger.valueOf((long)(multiplier * 10000)))
                     .divide(BigInteger.valueOf(10000));
    }
}
