package com.novaswap.service;

import com.novaswap.model.event.BurnEvent;
import com.novaswap.model.event.MintEvent;
import com.novaswap.model.event.SwapEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 事件监听服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventListenerService {
    
    private final Web3j web3j;
    
    // Swap事件签名
    private static final Event SWAP_EVENT = new Event("Swap",
        Arrays.asList(
            new TypeReference<Address>(true) {},
            new TypeReference<Address>(true) {}
        )
    );
    
    // Mint事件签名
    private static final Event MINT_EVENT = new Event("Mint",
        Arrays.asList(new TypeReference<Address>(true) {})
    );
    
    // Burn事件签名
    private static final Event BURN_EVENT = new Event("Burn",
        Arrays.asList(
            new TypeReference<Address>(true) {},
            new TypeReference<Address>(true) {}
        )
    );
    
    /**
     * 获取Swap事件
     */
    public List<SwapEvent> getSwapEvents(String pairAddress, BigInteger fromBlock, BigInteger toBlock) {
        try {
            String eventSignature = EventEncoder.encode(SWAP_EVENT);
            
            EthFilter filter = new EthFilter(
                org.web3j.protocol.core.DefaultBlockParameter.valueOf(fromBlock),
                org.web3j.protocol.core.DefaultBlockParameter.valueOf(toBlock),
                pairAddress
            );
            filter.addSingleTopic(eventSignature);
            
            EthLog ethLog = web3j.ethGetLogs(filter).send();
            List<SwapEvent> events = new ArrayList<>();
            
            for (EthLog.LogResult<?> logResult : ethLog.getLogs()) {
                Log eventLog = (Log) logResult.get();
                SwapEvent swapEvent = parseSwapEvent(eventLog);
                events.add(swapEvent);
            }
            
            log.info("Retrieved {} Swap events from block {} to {}", events.size(), fromBlock, toBlock);
            return events;
        } catch (Exception e) {
            log.error("Failed to get Swap events", e);
            throw new RuntimeException("Failed to get Swap events", e);
        }
    }
    
    /**
     * 获取Mint事件
     */
    public List<MintEvent> getMintEvents(String pairAddress, BigInteger fromBlock, BigInteger toBlock) {
        try {
            String eventSignature = EventEncoder.encode(MINT_EVENT);
            
            EthFilter filter = new EthFilter(
                org.web3j.protocol.core.DefaultBlockParameter.valueOf(fromBlock),
                org.web3j.protocol.core.DefaultBlockParameter.valueOf(toBlock),
                pairAddress
            );
            filter.addSingleTopic(eventSignature);
            
            EthLog ethLog = web3j.ethGetLogs(filter).send();
            List<MintEvent> events = new ArrayList<>();
            
            for (EthLog.LogResult<?> logResult : ethLog.getLogs()) {
                Log eventLog = (Log) logResult.get();
                MintEvent mintEvent = parseMintEvent(eventLog);
                events.add(mintEvent);
            }
            
            log.info("Retrieved {} Mint events from block {} to {}", events.size(), fromBlock, toBlock);
            return events;
        } catch (Exception e) {
            log.error("Failed to get Mint events", e);
            throw new RuntimeException("Failed to get Mint events", e);
        }
    }
    
    /**
     * 获取Burn事件
     */
    public List<BurnEvent> getBurnEvents(String pairAddress, BigInteger fromBlock, BigInteger toBlock) {
        try {
            String eventSignature = EventEncoder.encode(BURN_EVENT);
            
            EthFilter filter = new EthFilter(
                org.web3j.protocol.core.DefaultBlockParameter.valueOf(fromBlock),
                org.web3j.protocol.core.DefaultBlockParameter.valueOf(toBlock),
                pairAddress
            );
            filter.addSingleTopic(eventSignature);
            
            EthLog ethLog = web3j.ethGetLogs(filter).send();
            List<BurnEvent> events = new ArrayList<>();
            
            for (EthLog.LogResult<?> logResult : ethLog.getLogs()) {
                Log eventLog = (Log) logResult.get();
                BurnEvent burnEvent = parseBurnEvent(eventLog);
                events.add(burnEvent);
            }
            
            log.info("Retrieved {} Burn events from block {} to {}", events.size(), fromBlock, toBlock);
            return events;
        } catch (Exception e) {
            log.error("Failed to get Burn events", e);
            throw new RuntimeException("Failed to get Burn events", e);
        }
    }
    
    /**
     * 获取当前区块号
     */
    public BigInteger getCurrentBlockNumber() {
        try {
            return web3j.ethBlockNumber().send().getBlockNumber();
        } catch (Exception e) {
            log.error("Failed to get current block number", e);
            throw new RuntimeException("Failed to get current block number", e);
        }
    }
    
    private SwapEvent parseSwapEvent(Log log) {
        // 简化解析，实际应使用FunctionReturnDecoder
        return SwapEvent.builder()
            .txHash(log.getTransactionHash())
            .pairAddress(log.getAddress())
            .blockNumber(log.getBlockNumber())
            .timestamp(Instant.now()) // 实际应从区块获取
            .build();
    }
    
    private MintEvent parseMintEvent(Log log) {
        return MintEvent.builder()
            .txHash(log.getTransactionHash())
            .pairAddress(log.getAddress())
            .blockNumber(log.getBlockNumber())
            .timestamp(Instant.now())
            .build();
    }
    
    private BurnEvent parseBurnEvent(Log log) {
        return BurnEvent.builder()
            .txHash(log.getTransactionHash())
            .pairAddress(log.getAddress())
            .blockNumber(log.getBlockNumber())
            .timestamp(Instant.now())
            .build();
    }
}
