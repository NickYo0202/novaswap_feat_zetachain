package com.novaswap.service;

import com.novaswap.model.TokenBalance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {
    
    private final Web3j web3j;
    private final MulticallService multicallService;
    
    /**
     * 获取ETH余额
     */
    public BigInteger getEthBalance(String address) {
        try {
            return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .send()
                .getBalance();
        } catch (Exception e) {
            log.error("Failed to get ETH balance for {}", address, e);
            throw new RuntimeException("Failed to get ETH balance", e);
        }
    }
    
    /**
     * 获取ERC20代币余额
     */
    public BigInteger getTokenBalance(String tokenAddress, String userAddress) {
        try {
            Function function = new Function(
                "balanceOf",
                Arrays.asList(new Address(userAddress)),
                Arrays.asList(new TypeReference<Uint256>() {})
            );
            
            String encodedFunction = FunctionEncoder.encode(function);
            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(userAddress, tokenAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send();
            
            List<Type> result = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
            );
            
            return (BigInteger) result.get(0).getValue();
        } catch (Exception e) {
            log.error("Failed to get token balance for {} at {}", userAddress, tokenAddress, e);
            throw new RuntimeException("Failed to get token balance", e);
        }
    }
    
    /**
     * 获取代币信息（名称、符号、精度）
     */
    public TokenBalance getTokenInfo(String tokenAddress, String userAddress) {
        try {
            String name = getTokenName(tokenAddress);
            String symbol = getTokenSymbol(tokenAddress);
            int decimals = getTokenDecimals(tokenAddress);
            BigInteger balance = getTokenBalance(tokenAddress, userAddress);
            
            String formattedBalance = formatBalance(balance, decimals);
            
            return new TokenBalance(tokenAddress, symbol, name, decimals, balance, formattedBalance);
        } catch (Exception e) {
            log.error("Failed to get token info for {}", tokenAddress, e);
            throw new RuntimeException("Failed to get token info", e);
        }
    }
    
    /**
     * 批量获取多个代币的余额（使用Multicall优化）
     */
    public List<TokenBalance> getMultipleTokenBalances(List<String> tokenAddresses, String userAddress) {
        try {
            log.debug("Getting balances for {} tokens using Multicall", tokenAddresses.size());
            
            // 使用Multicall批量获取余额
            List<BigInteger> balances = multicallService.getBalances(tokenAddresses, userAddress).join();
            
            List<TokenBalance> results = new ArrayList<>();
            for (int i = 0; i < tokenAddresses.size(); i++) {
                String tokenAddress = tokenAddresses.get(i);
                BigInteger balance = balances.get(i);
                
                try {
                    // 获取代币信息（这里可以进一步优化为Multicall）
                    String name = getTokenName(tokenAddress);
                    String symbol = getTokenSymbol(tokenAddress);
                    int decimals = getTokenDecimals(tokenAddress);
                    String formattedBalance = formatBalance(balance, decimals);
                    
                    results.add(new TokenBalance(tokenAddress, symbol, name, decimals, balance, formattedBalance));
                } catch (Exception e) {
                    log.warn("Failed to get token info for {}, using defaults", tokenAddress);
                    results.add(new TokenBalance(tokenAddress, "???", "Unknown", 18, balance, 
                            formatBalance(balance, 18)));
                }
            }
            
            return results;
        } catch (Exception e) {
            log.error("Multicall failed, falling back to sequential queries", e);
            // 降级到逐个查询
            return getMultipleTokenBalancesSequential(tokenAddresses, userAddress);
        }
    }
    
    /**
     * 批量获取多个代币的余额（顺序查询，降级方案）
     */
    private List<TokenBalance> getMultipleTokenBalancesSequential(List<String> tokenAddresses, String userAddress) {
        List<TokenBalance> balances = new ArrayList<>();
        
        for (String tokenAddress : tokenAddresses) {
            try {
                TokenBalance balance = getTokenInfo(tokenAddress, userAddress);
                balances.add(balance);
            } catch (Exception e) {
                log.warn("Failed to get balance for token {}, skipping", tokenAddress);
            }
        }
        
        return balances;
    }
    
    private String getTokenName(String tokenAddress) {
        try {
            Function function = new Function(
                "name",
                Arrays.asList(),
                Arrays.asList(new TypeReference<Utf8String>() {})
            );
            
            String encodedFunction = FunctionEncoder.encode(function);
            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, tokenAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send();
            
            List<Type> result = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
            );
            
            return (String) result.get(0).getValue();
        } catch (Exception e) {
            log.warn("Failed to get token name for {}", tokenAddress);
            return "Unknown";
        }
    }
    
    private String getTokenSymbol(String tokenAddress) {
        try {
            Function function = new Function(
                "symbol",
                Arrays.asList(),
                Arrays.asList(new TypeReference<Utf8String>() {})
            );
            
            String encodedFunction = FunctionEncoder.encode(function);
            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, tokenAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send();
            
            List<Type> result = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
            );
            
            return (String) result.get(0).getValue();
        } catch (Exception e) {
            log.warn("Failed to get token symbol for {}", tokenAddress);
            return "???";
        }
    }
    
    private int getTokenDecimals(String tokenAddress) {
        try {
            Function function = new Function(
                "decimals",
                Arrays.asList(),
                Arrays.asList(new TypeReference<Uint8>() {})
            );
            
            String encodedFunction = FunctionEncoder.encode(function);
            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, tokenAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send();
            
            List<Type> result = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
            );
            
            return ((BigInteger) result.get(0).getValue()).intValue();
        } catch (Exception e) {
            log.warn("Failed to get token decimals for {}", tokenAddress);
            return 18;
        }
    }
    
    private String formatBalance(BigInteger balance, int decimals) {
        BigDecimal balanceDecimal = new BigDecimal(balance);
        BigDecimal divisor = BigDecimal.TEN.pow(decimals);
        return balanceDecimal.divide(divisor, 6, RoundingMode.DOWN).stripTrailingZeros().toPlainString();
    }
}
