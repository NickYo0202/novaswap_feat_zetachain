package com.novaswap.service.crosschain;

import com.novaswap.model.crosschain.CrossChainRoute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

/**
 * 跨链费用计算服务
 * 计算源链gas、桥接费、目标链gas、服务费等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrossChainFeeService {

    private final ZetaChainService zetaChainService;
    
    // 服务费率
    private static final BigDecimal SERVICE_FEE_PERCENT = new BigDecimal("0.0005"); // 0.05%
    
    // Gas价格估算（单位：wei）
    private static final BigInteger BASE_GAS_PRICE = new BigInteger("30000000000"); // 30 gwei
    
    // 操作gas消耗估算
    private static final BigInteger SWAP_GAS_LIMIT = new BigInteger("150000");
    private static final BigInteger BRIDGE_GAS_LIMIT = new BigInteger("200000");
    private static final BigInteger RELAY_GAS_LIMIT = new BigInteger("100000");

    /**
     * 计算费用分解
     */
    public CrossChainRoute.FeeBreakdown calculateFeeBreakdown(
            Integer sourceChainId,
            Integer targetChainId,
            BigInteger amountIn,
            List<CrossChainRoute.RouteStep> steps) {
        
        BigInteger sourceGasFee = calculateSourceChainGasFee(sourceChainId, steps);
        BigInteger bridgeFee = zetaChainService.getBridgeFee(sourceChainId, targetChainId);
        BigInteger targetGasFee = calculateTargetChainGasFee(targetChainId, steps);
        BigInteger serviceFee = calculateServiceFee(amountIn);
        
        // 第三方桥接费（透传）
        BigInteger thirdPartyFee = BigInteger.ZERO;
        for (CrossChainRoute.RouteStep step : steps) {
            if (step.getType() == CrossChainRoute.StepType.BRIDGE) {
                thirdPartyFee = thirdPartyFee.add(step.getFee());
            }
        }
        
        BigInteger totalFee = sourceGasFee
                .add(bridgeFee)
                .add(targetGasFee)
                .add(serviceFee)
                .add(thirdPartyFee);
        
        return CrossChainRoute.FeeBreakdown.builder()
                .sourceChainGasFee(sourceGasFee)
                .bridgeFee(bridgeFee)
                .targetChainGasFee(targetGasFee)
                .serviceFee(serviceFee)
                .thirdPartyFee(thirdPartyFee)
                .totalFee(totalFee)
                .build();
    }

    /**
     * 计算源链gas费用
     */
    private BigInteger calculateSourceChainGasFee(
            Integer chainId, 
            List<CrossChainRoute.RouteStep> steps) {
        
        BigInteger totalGas = BigInteger.ZERO;
        
        for (CrossChainRoute.RouteStep step : steps) {
            if (step.getChainId().equals(chainId)) {
                switch (step.getType()) {
                    case SWAP:
                        totalGas = totalGas.add(SWAP_GAS_LIMIT);
                        break;
                    case BRIDGE:
                        totalGas = totalGas.add(BRIDGE_GAS_LIMIT);
                        break;
                    case RECEIVE:
                        // RECEIVE不需要源链gas
                        break;
                }
            }
        }
        
        BigInteger gasPrice = getGasPrice(chainId);
        return totalGas.multiply(gasPrice);
    }

    /**
     * 计算目标链gas费用
     */
    private BigInteger calculateTargetChainGasFee(
            Integer chainId,
            List<CrossChainRoute.RouteStep> steps) {
        
        BigInteger totalGas = BigInteger.ZERO;
        
        for (CrossChainRoute.RouteStep step : steps) {
            if (step.getChainId().equals(chainId)) {
                switch (step.getType()) {
                    case SWAP:
                        totalGas = totalGas.add(SWAP_GAS_LIMIT);
                        break;
                    case RECEIVE:
                        totalGas = totalGas.add(RELAY_GAS_LIMIT);
                        break;
                    case BRIDGE:
                        // BRIDGE不在目标链执行
                        break;
                }
            }
        }
        
        BigInteger gasPrice = getGasPrice(chainId);
        return totalGas.multiply(gasPrice);
    }

    /**
     * 计算服务费
     */
    private BigInteger calculateServiceFee(BigInteger amountIn) {
        BigDecimal amount = new BigDecimal(amountIn);
        BigDecimal fee = amount.multiply(SERVICE_FEE_PERCENT);
        return fee.toBigInteger();
    }

    /**
     * 获取链的gas价格
     */
    private BigInteger getGasPrice(Integer chainId) {
        // 不同链的gas价格不同
        switch (chainId) {
            case 1: // Ethereum
                return BASE_GAS_PRICE.multiply(BigInteger.valueOf(2));
            case 56: // BSC
                return BASE_GAS_PRICE.divide(BigInteger.valueOf(3));
            case 137: // Polygon
                return BASE_GAS_PRICE.divide(BigInteger.valueOf(2));
            default:
                return BASE_GAS_PRICE;
        }
    }

    /**
     * 计算任意代币支付的等值费用
     * @param feeInNativeToken 原生代币计价的费用
     * @param paymentToken 支付代币地址
     * @param nativeTokenPrice 原生代币价格（USD）
     * @param paymentTokenPrice 支付代币价格（USD）
     * @return 支付代币数量
     */
    public BigInteger calculateFeeInToken(
            BigInteger feeInNativeToken,
            String paymentToken,
            BigDecimal nativeTokenPrice,
            BigDecimal paymentTokenPrice) {
        
        // 计算USD价值
        BigDecimal feeInUsd = new BigDecimal(feeInNativeToken)
                .multiply(nativeTokenPrice)
                .divide(BigDecimal.TEN.pow(18), 18, RoundingMode.HALF_UP);
        
        // 转换为支付代币数量
        BigDecimal amountInPaymentToken = feeInUsd
                .divide(paymentTokenPrice, 18, RoundingMode.HALF_UP)
                .multiply(BigDecimal.TEN.pow(18));
        
        return amountInPaymentToken.toBigInteger();
    }

    /**
     * 计算中继服务费用（目标链gas代付）
     * @param targetGasFee 目标链gas费用
     * @return 中继服务费（最多5%）
     */
    public BigInteger calculateRelayServiceFee(BigInteger targetGasFee) {
        BigDecimal fee = new BigDecimal(targetGasFee);
        BigDecimal relayFee = fee.multiply(new BigDecimal("0.05")); // 5%
        return relayFee.toBigInteger();
    }

    /**
     * 计算退款金额
     * @param paidAmount 已支付金额
     * @param actualFee 实际使用费用
     * @return 退款金额
     */
    public BigInteger calculateRefund(BigInteger paidAmount, BigInteger actualFee) {
        BigInteger refund = paidAmount.subtract(actualFee);
        return refund.compareTo(BigInteger.ZERO) > 0 ? refund : BigInteger.ZERO;
    }

    /**
     * 估算总费用（USD）
     */
    public BigDecimal estimateTotalFeeInUsd(
            CrossChainRoute.FeeBreakdown feeBreakdown,
            BigDecimal nativeTokenPriceUsd) {
        
        BigDecimal totalFeeInToken = new BigDecimal(feeBreakdown.getTotalFee());
        BigDecimal totalFeeInUsd = totalFeeInToken
                .multiply(nativeTokenPriceUsd)
                .divide(BigDecimal.TEN.pow(18), 2, RoundingMode.HALF_UP);
        
        return totalFeeInUsd;
    }

    /**
     * 验证费用是否合理
     */
    public boolean validateFee(
            BigInteger feeAmount,
            BigInteger transactionAmount) {
        
        // 费用不应超过交易金额的10%
        BigDecimal feePercent = new BigDecimal(feeAmount)
                .divide(new BigDecimal(transactionAmount), 4, RoundingMode.HALF_UP);
        
        return feePercent.compareTo(new BigDecimal("0.1")) <= 0;
    }

    /**
     * 获取费用明细描述
     */
    public String getFeeBreakdownDescription(CrossChainRoute.FeeBreakdown feeBreakdown) {
        return String.format(
                "Source Gas: %s wei, Bridge: %s wei, Target Gas: %s wei, Service: %s wei, Total: %s wei",
                feeBreakdown.getSourceChainGasFee(),
                feeBreakdown.getBridgeFee(),
                feeBreakdown.getTargetChainGasFee(),
                feeBreakdown.getServiceFee(),
                feeBreakdown.getTotalFee()
        );
    }
}
