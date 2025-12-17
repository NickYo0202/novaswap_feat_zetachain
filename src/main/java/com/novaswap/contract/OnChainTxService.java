package com.novaswap.contract;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Numeric;

@Service
public class OnChainTxService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractGasProvider gasProvider;
    private final long chainId;

    public OnChainTxService(Web3j web3j,
                           Credentials credentials,
                           ContractGasProvider gasProvider,
                           @Value("${novaswap.chainId:1}") long chainId) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.gasProvider = gasProvider;
        this.chainId = chainId;
    }

    public CompletableFuture<String> sendRawTransaction(String to, BigInteger value, String data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BigInteger nonce = fetchNonce(credentials.getAddress());
                RawTransaction rawTx = RawTransaction.createTransaction(
                        nonce,
                        gasProvider.getGasPrice(),
                        gasProvider.getGasLimit(),
                        to,
                        value,
                        data);
                byte[] signed = TransactionEncoder.signMessage(rawTx, chainId, credentials);
                String hexValue = Numeric.toHexString(signed);
                EthSendTransaction sent = web3j.ethSendRawTransaction(hexValue).send();
                if (sent.hasError()) {
                    throw new IllegalStateException("Tx error: " + sent.getError().getMessage());
                }
                return sent.getTransactionHash();
            } catch (IOException e) {
                throw new RuntimeException("Send tx failed", e);
            }
        });
    }

    private BigInteger fetchNonce(String address) throws IOException {
        EthGetTransactionCount txCount = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send();
        return txCount.getTransactionCount();
    }

    public CompletableFuture<String> sendEthTransfer(String to, BigInteger valueWei) {
        Transaction tx = Transaction.createEtherTransaction(
                credentials.getAddress(), null, gasProvider.getGasPrice(), gasProvider.getGasLimit(), to, valueWei);
        return CompletableFuture.supplyAsync(() -> {
            try {
                EthSendTransaction sent = web3j.ethSendTransaction(tx).send();
                if (sent.hasError()) {
                    throw new IllegalStateException("Tx error: " + sent.getError().getMessage());
                }
                return sent.getTransactionHash();
            } catch (IOException e) {
                throw new RuntimeException("Send ETH failed", e);
            }
        });
    }
}
