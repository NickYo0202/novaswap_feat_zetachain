package com.novaswap.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllowanceManagementService {
    
    private final Web3j web3j;
    
    // 无限授权额度
    private static final BigInteger MAX_ALLOWANCE = new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
    
    /**
     * 检查授权额度
     */
    public BigInteger checkAllowance(String tokenAddress, String owner, String spender) {
        try {
            Function function = new Function(
                "allowance",
                Arrays.asList(new Address(owner), new Address(spender)),
                Arrays.asList(new TypeReference<Uint256>() {})
            );
            
            String encodedFunction = FunctionEncoder.encode(function);
            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(owner, tokenAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send();
            
            List<Type> result = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
            );
            
            return (BigInteger) result.get(0).getValue();
        } catch (Exception e) {
            log.error("Failed to check allowance for token {} from {} to {}", 
                tokenAddress, owner, spender, e);
            throw new RuntimeException("Failed to check allowance", e);
        }
    }
    
    /**
     * 检查是否需要授权
     */
    public boolean needsApproval(String tokenAddress, String owner, String spender, BigInteger amount) {
        BigInteger currentAllowance = checkAllowance(tokenAddress, owner, spender);
        return currentAllowance.compareTo(amount) < 0;
    }
    
    /**
     * 批量检查双币授权状态
     */
    public Map<String, Boolean> checkDualTokenApproval(
        String token0, String token1, String owner, String spender, 
        BigInteger amount0, BigInteger amount1
    ) {
        Map<String, Boolean> approvalStatus = new HashMap<>();
        
        boolean token0NeedsApproval = needsApproval(token0, owner, spender, amount0);
        boolean token1NeedsApproval = needsApproval(token1, owner, spender, amount1);
        
        approvalStatus.put(token0, token0NeedsApproval);
        approvalStatus.put(token1, token1NeedsApproval);
        
        log.info("Dual token approval check - Token0: {} needs approval: {}, Token1: {} needs approval: {}",
            token0, token0NeedsApproval, token1, token1NeedsApproval);
        
        return approvalStatus;
    }
    
    /**
     * 生成approve交易数据
     */
    public String buildApproveCalldata(String spender, BigInteger amount) {
        Function function = new Function(
            "approve",
            Arrays.asList(new Address(spender), new Uint256(amount)),
            Arrays.asList()
        );
        
        return FunctionEncoder.encode(function);
    }
    
    /**
     * 生成无限授权交易数据
     */
    public String buildInfiniteApproveCalldata(String spender) {
        return buildApproveCalldata(spender, MAX_ALLOWANCE);
    }
    
    /**
     * 获取建议授权额度（精确额度 vs 无限授权）
     */
    public BigInteger getRecommendedApprovalAmount(BigInteger requiredAmount, boolean useInfiniteApproval) {
        if (useInfiniteApproval) {
            return MAX_ALLOWANCE;
        }
        // 建议授权额度为需求量的1.1倍，避免精度问题
        return requiredAmount.multiply(BigInteger.valueOf(110)).divide(BigInteger.valueOf(100));
    }
    
    /**
     * 检查并返回需要授权的代币列表
     */
    public List<String> getTokensNeedingApproval(
        List<String> tokens, 
        List<BigInteger> amounts, 
        String owner, 
        String spender
    ) {
        if (tokens.size() != amounts.size()) {
            throw new IllegalArgumentException("Tokens and amounts lists must have the same size");
        }
        
        return tokens.stream()
            .filter(token -> {
                int index = tokens.indexOf(token);
                return needsApproval(token, owner, spender, amounts.get(index));
            })
            .toList();
    }
}
