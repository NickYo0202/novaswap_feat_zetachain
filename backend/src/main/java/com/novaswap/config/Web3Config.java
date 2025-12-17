package com.novaswap.config;

import java.math.BigInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

@Configuration
public class Web3Config {

    @Value("${novaswap.rpcUrl:http://localhost:8545}")
    private String rpcUrl;

    @Value("${novaswap.chainId:1}")
    private long chainId;

    @Value("${novaswap.privateKey:}")
    private String privateKey;

    @Value("${novaswap.gasPriceGwei:15}")
    private BigInteger gasPriceGwei;

    @Value("${novaswap.gasLimit:2_000_000}")
    private BigInteger gasLimit;

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(rpcUrl));
    }

    @Bean
    public Credentials credentials() {
        if (privateKey == null || privateKey.isEmpty()) {
            throw new IllegalStateException("Private key not configured: set novaswap.privateKey");
        }
        return Credentials.create(privateKey);
    }

    @Bean
    public ContractGasProvider gasProvider() {
        BigInteger weiPrice = gasPriceGwei.multiply(BigInteger.valueOf(1_000_000_000L));
        return new StaticGasProvider(weiPrice, gasLimit);
    }

    public long getChainId() {
        return chainId;
    }
}
