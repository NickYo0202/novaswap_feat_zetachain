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
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 工厂服务 - 用于获取交易对地址
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactoryService {
    
    private final Web3j web3j;
    
    /**
     * 通过Factory合约获取pair地址
     * 需要配置Factory合约地址
     */
    public String getPairAddress(String factoryAddress, String tokenA, String tokenB) {
        try {
            Function function = new Function(
                "getPair",
                Arrays.asList(new Address(tokenA), new Address(tokenB)),
                Collections.singletonList(new TypeReference<Address>() {})
            );
            
            String encodedFunction = FunctionEncoder.encode(function);
            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, factoryAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send();
            
            List<Type> result = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
            );
            
            String pairAddress = (String) result.get(0).getValue();
            
            // 检查是否为零地址（表示pair不存在）
            if (pairAddress.equals("0x0000000000000000000000000000000000000000")) {
                throw new RuntimeException("Pair does not exist for tokens: " + tokenA + " and " + tokenB);
            }
            
            return pairAddress;
        } catch (Exception e) {
            log.error("Failed to get pair address for {} and {}", tokenA, tokenB, e);
            throw new RuntimeException("Failed to get pair address", e);
        }
    }
    
    /**
     * 计算pair地址（使用CREATE2确定性部署）
     * 这是一个辅助方法，可以在不调用链上合约的情况下计算pair地址
     */
    public String calculatePairAddress(
        String factoryAddress,
        String tokenA,
        String tokenB,
        String initCodeHash
    ) {
        // 确保tokenA < tokenB
        if (tokenA.compareToIgnoreCase(tokenB) > 0) {
            String temp = tokenA;
            tokenA = tokenB;
            tokenB = temp;
        }
        
        // 使用CREATE2公式计算地址
        // address = keccak256(0xff ++ factory ++ salt ++ keccak256(initCode))[12:]
        // 这里需要实现具体的计算逻辑
        
        log.warn("calculatePairAddress is not yet implemented, falling back to getPairAddress");
        throw new UnsupportedOperationException("calculatePairAddress not implemented");
    }
}
