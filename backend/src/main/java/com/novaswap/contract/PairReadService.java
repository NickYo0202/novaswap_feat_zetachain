package com.novaswap.contract;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint112;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

@Service
public class PairReadService {

    public record Reserves(BigInteger reserve0, BigInteger reserve1, BigInteger blockTimestampLast) {}

    private final Web3j web3j;

    public PairReadService(Web3j web3j) {
        this.web3j = web3j;
    }

    public Reserves getReserves(String pairAddress) {
        Function fn = new Function(
                "getReserves",
                Collections.emptyList(),
                Arrays.asList(new TypeReference<Uint112>() {}, new TypeReference<Uint112>() {}, new TypeReference<Uint256>() {}));
        String data = FunctionEncoder.encode(fn);
        Transaction callTx = Transaction.createEthCallTransaction(null, pairAddress, data);
        try {
            EthCall response = web3j.ethCall(callTx, DefaultBlockParameterName.LATEST).send();
            if (response.isReverted()) {
                throw new IllegalStateException("getReserves reverted: " + response.getRevertReason());
            }
            var values = org.web3j.abi.FunctionReturnDecoder.decode(response.getValue(), fn.getOutputParameters());
            BigInteger r0 = (BigInteger) values.get(0).getValue();
            BigInteger r1 = (BigInteger) values.get(1).getValue();
            BigInteger ts = (BigInteger) values.get(2).getValue();
            return new Reserves(r0, r1, ts);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read reserves", e);
        }
    }

    public BigInteger getTotalSupply(String pairAddress) {
        Function fn = new Function(
                "totalSupply",
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Uint256>() {}));
        String data = FunctionEncoder.encode(fn);
        Transaction callTx = Transaction.createEthCallTransaction(null, pairAddress, data);
        try {
            EthCall response = web3j.ethCall(callTx, DefaultBlockParameterName.LATEST).send();
            if (response.isReverted()) {
                throw new IllegalStateException("totalSupply reverted: " + response.getRevertReason());
            }
            return new java.math.BigInteger(response.getValue().substring(2), 16);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read totalSupply", e);
        }
    }
}
