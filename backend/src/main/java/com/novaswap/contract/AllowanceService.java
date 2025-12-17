package com.novaswap.contract;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

@Service
public class AllowanceService {

    private final Web3j web3j;
    private final OnChainTxService txService;

    public AllowanceService(Web3j web3j, OnChainTxService txService) {
        this.web3j = web3j;
        this.txService = txService;
    }

    public BigInteger getAllowance(String token, String owner, String spender) {
        Function fn = new Function(
                "allowance",
                Arrays.asList(new Address(owner), new Address(spender)),
                Collections.singletonList(new TypeReference<Uint256>() {}));
        String data = FunctionEncoder.encode(fn);
        Transaction callTx = Transaction.createEthCallTransaction(owner, token, data);
        try {
            EthCall response = web3j.ethCall(callTx, DefaultBlockParameterName.LATEST).send();
            if (response.isReverted()) {
                throw new IllegalStateException("Allowance call reverted: " + response.getRevertReason());
            }
            return new java.math.BigInteger(response.getValue().substring(2), 16);
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch allowance", e);
        }
    }

    public CompletableFuture<String> approve(String token, String spender, BigInteger amount) {
        Function fn = new Function(
                "approve",
                Arrays.asList(new Address(spender), new Uint256(amount)),
                Collections.emptyList());
        String data = FunctionEncoder.encode(fn);
        return txService.sendRawTransaction(token, BigInteger.ZERO, data);
    }
}
