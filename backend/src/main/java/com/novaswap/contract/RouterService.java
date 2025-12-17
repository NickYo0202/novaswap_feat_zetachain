package com.novaswap.contract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import com.novaswap.config.ContractProperties;

@Service
public class RouterService {

    private final OnChainTxService txService;
    private final ContractProperties contracts;

    public RouterService(OnChainTxService txService, ContractProperties contracts) {
        this.txService = txService;
        this.contracts = contracts;
    }

    public CompletableFuture<String> swapExactTokensForTokens(
            BigInteger amountIn,
            BigInteger amountOutMin,
            List<String> path,
            String to,
            BigInteger deadline) {
        Function fn = new Function(
                "swapExactTokensForTokens",
                Arrays.asList(
                        new Uint256(amountIn),
                        new Uint256(amountOutMin),
                        new DynamicArray<>(Address.class, path.stream().map(Address::new).collect(Collectors.toList())),
                        new Address(to),
                        new Uint256(deadline)),
                Collections.emptyList());
        String data = FunctionEncoder.encode(fn);
        return txService.sendRawTransaction(contracts.getRouter(), BigInteger.ZERO, data);
    }

    public CompletableFuture<String> addLiquidity(
            String tokenA,
            String tokenB,
            BigInteger amountADesired,
            BigInteger amountBDesired,
            BigInteger amountAMin,
            BigInteger amountBMin,
            String to,
            BigInteger deadline) {
        Function fn = new Function(
                "addLiquidity",
                Arrays.asList(
                        new Address(tokenA),
                        new Address(tokenB),
                        new Uint256(amountADesired),
                        new Uint256(amountBDesired),
                        new Uint256(amountAMin),
                        new Uint256(amountBMin),
                        new Address(to),
                        new Uint256(deadline)),
                Collections.emptyList());
        String data = FunctionEncoder.encode(fn);
        return txService.sendRawTransaction(contracts.getRouter(), BigInteger.ZERO, data);
    }

    public CompletableFuture<String> removeLiquidity(
            String tokenA,
            String tokenB,
            BigInteger liquidity,
            BigInteger amountAMin,
            BigInteger amountBMin,
            String to,
            BigInteger deadline) {
        Function fn = new Function(
                "removeLiquidity",
                Arrays.asList(
                        new Address(tokenA),
                        new Address(tokenB),
                        new Uint256(liquidity),
                        new Uint256(amountAMin),
                        new Uint256(amountBMin),
                        new Address(to),
                        new Uint256(deadline)),
                Collections.emptyList());
        String data = FunctionEncoder.encode(fn);
        return txService.sendRawTransaction(contracts.getRouter(), BigInteger.ZERO, data);
    }

    public static BigInteger getAmountOut(BigInteger amountIn, BigInteger reserveIn, BigInteger reserveOut) {
        if (amountIn.signum() <= 0) {
            throw new IllegalArgumentException("amountIn must be positive");
        }
        BigInteger feeNumerator = BigInteger.valueOf(997);
        BigInteger feeDenominator = BigInteger.valueOf(1000);
        BigInteger amountInWithFee = amountIn.multiply(feeNumerator);
        BigInteger numerator = amountInWithFee.multiply(reserveOut);
        BigInteger denominator = reserveIn.multiply(feeDenominator).add(amountInWithFee);
        return numerator.divide(denominator);
    }
}
