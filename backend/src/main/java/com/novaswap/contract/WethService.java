package com.novaswap.contract;

import java.math.BigInteger;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import com.novaswap.config.ContractProperties;

@Service
public class WethService {

    private final OnChainTxService txService;
    private final ContractProperties contracts;

    public WethService(OnChainTxService txService, ContractProperties contracts) {
        this.txService = txService;
        this.contracts = contracts;
    }

    public CompletableFuture<String> wrap(BigInteger amountWei) {
        Function deposit = new Function("deposit", Collections.emptyList(), Collections.emptyList());
        String data = FunctionEncoder.encode(deposit);
        return txService.sendRawTransaction(contracts.getWeth(), amountWei, data);
    }

    public CompletableFuture<String> unwrap(BigInteger amountWei) {
        Function withdraw = new Function(
                "withdraw",
                Collections.singletonList(new Uint256(amountWei)),
                Collections.emptyList());
        String data = FunctionEncoder.encode(withdraw);
        return txService.sendRawTransaction(contracts.getWeth(), BigInteger.ZERO, data);
    }
}
