package com.novaswap.api.controller;

import com.novaswap.api.dto.*;
import com.novaswap.service.TransactionBuilderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * 交易构建控制器 - 生成链上交易的calldata
 * 前端获取calldata后，使用钱包签名并发送交易
 */
@Slf4j
@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
@Validated
@Tag(name = "Transaction Builder", description = "交易构建API - 生成swap/addLiquidity/removeLiquidity的calldata")
public class TransactionBuilderController {

    private final TransactionBuilderService transactionBuilder;
    
    private static final String ETH_ADDRESS = "0x0000000000000000000000000000000000000000";

    @PostMapping("/build/swap")
    @Operation(summary = "构建兑换交易", 
               description = "生成代币兑换的calldata，支持代币-代币、ETH-代币、代币-ETH兑换")
    public TransactionResponse buildSwapTransaction(@Valid @RequestBody SwapTransactionRequest request) {
        log.info("Building swap transaction: {} -> {}, amount: {}", 
                request.getTokenIn(), request.getTokenOut(), request.getAmountIn());

        BigInteger deadline = transactionBuilder.calculateDeadline(request.getDeadlineMinutes());
        
        // 确定使用的路径
        boolean hasPath = request.getPath() != null && !request.getPath().isEmpty();
        
        boolean isETHIn = ETH_ADDRESS.equalsIgnoreCase(request.getTokenIn());
        boolean isETHOut = ETH_ADDRESS.equalsIgnoreCase(request.getTokenOut());

        String calldata;
        String functionName;
        String value = "0";
        
        if (isETHIn && !isETHOut) {
            // ETH -> Token
            BigInteger amountOutMin = request.getAmountOutMin() != null ? 
                    request.getAmountOutMin() : 
                    transactionBuilder.calculateMinAmount(request.getAmountIn(), request.getSlippagePercent());
            
            calldata = transactionBuilder.buildSwapExactETHForTokens(
                    amountOutMin,
                    hasPath ? request.getPath() : Arrays.asList(getWETHAddress(), request.getTokenOut()),
                    request.getRecipient(),
                    deadline
            );
            functionName = "swapExactETHForTokens";
            value = request.getAmountIn().toString();
            
        } else if (!isETHIn && isETHOut) {
            // Token -> ETH
            BigInteger amountOutMin = request.getAmountOutMin() != null ? 
                    request.getAmountOutMin() : 
                    transactionBuilder.calculateMinAmount(request.getAmountIn(), request.getSlippagePercent());
            
            calldata = transactionBuilder.buildSwapExactTokensForETH(
                    request.getAmountIn(),
                    amountOutMin,
                    hasPath ? request.getPath() : Arrays.asList(request.getTokenIn(), getWETHAddress()),
                    request.getRecipient(),
                    deadline
            );
            functionName = "swapExactTokensForETH";
            
        } else {
            // Token -> Token
            BigInteger amountOutMin = request.getAmountOutMin() != null ? 
                    request.getAmountOutMin() : 
                    transactionBuilder.calculateMinAmount(request.getAmountIn(), request.getSlippagePercent());
            
            calldata = transactionBuilder.buildSwapExactTokensForTokens(
                    request.getAmountIn(),
                    amountOutMin,
                    hasPath ? request.getPath() : Arrays.asList(request.getTokenIn(), request.getTokenOut()),
                    request.getRecipient(),
                    deadline
            );
            functionName = "swapExactTokensForTokens";
        }

        return TransactionResponse.builder()
                .to(transactionBuilder.getRouterAddress())
                .data(calldata)
                .value(value)
                .functionName(functionName)
                .deadline(deadline)
                .description(String.format("Swap %s for %s", request.getTokenIn(), request.getTokenOut()))
                .build();
    }

    @PostMapping("/build/add-liquidity")
    @Operation(summary = "构建添加流动性交易", 
               description = "生成添加流动性的calldata，支持代币对和ETH对")
    public TransactionResponse buildAddLiquidityTransaction(@Valid @RequestBody AddLiquidityTransactionRequest request) {
        log.info("Building add liquidity transaction: {} + {}", request.getTokenA(), request.getTokenB());

        BigInteger deadline = transactionBuilder.calculateDeadline(request.getDeadlineMinutes());
        
        boolean isETHPair = ETH_ADDRESS.equalsIgnoreCase(request.getTokenB());
        
        String calldata;
        String functionName;
        String value = "0";
        
        if (isETHPair) {
            // Token + ETH
            BigInteger amountTokenMin = request.getAmountAMin() != null ?
                    request.getAmountAMin() :
                    transactionBuilder.calculateMinAmount(request.getAmountADesired(), request.getSlippagePercent());
            
            BigInteger amountETHMin = request.getAmountBMin() != null ?
                    request.getAmountBMin() :
                    transactionBuilder.calculateMinAmount(request.getAmountBDesired(), request.getSlippagePercent());
            
            calldata = transactionBuilder.buildAddLiquidityETH(
                    request.getTokenA(),
                    request.getAmountADesired(),
                    amountTokenMin,
                    amountETHMin,
                    request.getRecipient(),
                    deadline
            );
            functionName = "addLiquidityETH";
            value = request.getAmountBDesired().toString();
            
        } else {
            // Token + Token
            BigInteger amountAMin = request.getAmountAMin() != null ?
                    request.getAmountAMin() :
                    transactionBuilder.calculateMinAmount(request.getAmountADesired(), request.getSlippagePercent());
            
            BigInteger amountBMin = request.getAmountBMin() != null ?
                    request.getAmountBMin() :
                    transactionBuilder.calculateMinAmount(request.getAmountBDesired(), request.getSlippagePercent());
            
            calldata = transactionBuilder.buildAddLiquidity(
                    request.getTokenA(),
                    request.getTokenB(),
                    request.getAmountADesired(),
                    request.getAmountBDesired(),
                    amountAMin,
                    amountBMin,
                    request.getRecipient(),
                    deadline
            );
            functionName = "addLiquidity";
        }

        return TransactionResponse.builder()
                .to(transactionBuilder.getRouterAddress())
                .data(calldata)
                .value(value)
                .functionName(functionName)
                .deadline(deadline)
                .description(String.format("Add liquidity: %s + %s", request.getTokenA(), request.getTokenB()))
                .build();
    }

    @PostMapping("/build/remove-liquidity")
    @Operation(summary = "构建移除流动性交易", 
               description = "生成移除流动性的calldata，支持代币对和ETH对")
    public TransactionResponse buildRemoveLiquidityTransaction(@Valid @RequestBody RemoveLiquidityTransactionRequest request) {
        log.info("Building remove liquidity transaction: {} + {}, liquidity: {}", 
                request.getTokenA(), request.getTokenB(), request.getLiquidity());

        BigInteger deadline = transactionBuilder.calculateDeadline(request.getDeadlineMinutes());
        
        boolean isETHPair = ETH_ADDRESS.equalsIgnoreCase(request.getTokenB());
        
        String calldata;
        String functionName;
        
        if (isETHPair) {
            // Token + ETH
            BigInteger amountTokenMin = request.getAmountAMin() != null ?
                    request.getAmountAMin() :
                    BigInteger.ZERO;
            
            BigInteger amountETHMin = request.getAmountBMin() != null ?
                    request.getAmountBMin() :
                    BigInteger.ZERO;
            
            calldata = transactionBuilder.buildRemoveLiquidityETH(
                    request.getTokenA(),
                    request.getLiquidity(),
                    amountTokenMin,
                    amountETHMin,
                    request.getRecipient(),
                    deadline
            );
            functionName = "removeLiquidityETH";
            
        } else {
            // Token + Token
            BigInteger amountAMin = request.getAmountAMin() != null ?
                    request.getAmountAMin() :
                    BigInteger.ZERO;
            
            BigInteger amountBMin = request.getAmountBMin() != null ?
                    request.getAmountBMin() :
                    BigInteger.ZERO;
            
            calldata = transactionBuilder.buildRemoveLiquidity(
                    request.getTokenA(),
                    request.getTokenB(),
                    request.getLiquidity(),
                    amountAMin,
                    amountBMin,
                    request.getRecipient(),
                    deadline
            );
            functionName = "removeLiquidity";
        }

        return TransactionResponse.builder()
                .to(transactionBuilder.getRouterAddress())
                .data(calldata)
                .value("0")
                .functionName(functionName)
                .deadline(deadline)
                .description(String.format("Remove liquidity: %s + %s", request.getTokenA(), request.getTokenB()))
                .build();
    }

    @GetMapping("/router-address")
    @Operation(summary = "获取Router合约地址", 
               description = "返回当前使用的Router合约地址")
    public String getRouterAddress() {
        return transactionBuilder.getRouterAddress();
    }

    @PostMapping("/calculate-deadline")
    @Operation(summary = "计算交易截止时间", 
               description = "根据当前时间和指定分钟数计算截止时间戳")
    public BigInteger calculateDeadline(@RequestParam(defaultValue = "20") int minutesFromNow) {
        return transactionBuilder.calculateDeadline(minutesFromNow);
    }

    @PostMapping("/calculate-slippage")
    @Operation(summary = "计算滑点数量", 
               description = "根据预期数量和滑点百分比计算最小/最大数量")
    public SlippageCalculationResponse calculateSlippage(
            @RequestParam String amount,
            @RequestParam(defaultValue = "0.5") double slippagePercent) {
        
        BigInteger amountBigInt = new BigInteger(amount);
        BigInteger minAmount = transactionBuilder.calculateMinAmount(amountBigInt, slippagePercent);
        BigInteger maxAmount = transactionBuilder.calculateMaxAmount(amountBigInt, slippagePercent);
        
        return new SlippageCalculationResponse(amount, minAmount.toString(), maxAmount.toString(), slippagePercent);
    }

    /**
     * 获取WETH地址（应该从配置读取）
     */
    private String getWETHAddress() {
        // TODO: 从配置文件读取
        return "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"; // Ethereum Mainnet WETH
    }

    /**
     * 滑点计算响应
     */
    record SlippageCalculationResponse(
            String originalAmount,
            String minAmount,
            String maxAmount,
            double slippagePercent
    ) {}
}
