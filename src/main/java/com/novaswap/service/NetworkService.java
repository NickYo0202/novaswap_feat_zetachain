package com.novaswap.service;

import com.novaswap.config.Web3Config;
import com.novaswap.model.Network;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthChainId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkService {
    
    private final Web3j web3j;
    private final Web3Config web3Config;
    
    private static final Map<Long, Network> SUPPORTED_NETWORKS = new HashMap<>();
    
    static {
        SUPPORTED_NETWORKS.put(1L, Network.ethereum());
        SUPPORTED_NETWORKS.put(42161L, Network.arbitrum());
        SUPPORTED_NETWORKS.put(10L, Network.optimism());
        SUPPORTED_NETWORKS.put(137L, Network.polygon());
        SUPPORTED_NETWORKS.put(56L, Network.bsc());
        SUPPORTED_NETWORKS.put(43114L, Network.avalanche());
        SUPPORTED_NETWORKS.put(7000L, Network.zetaChain());
    }
    
    public List<Network> getSupportedNetworks() {
        return new ArrayList<>(SUPPORTED_NETWORKS.values());
    }
    
    public Network getNetworkById(Long chainId) {
        Network network = SUPPORTED_NETWORKS.get(chainId);
        if (network == null) {
            throw new IllegalArgumentException("Unsupported network: " + chainId);
        }
        return network;
    }
    
    public Network getCurrentNetwork() {
        try {
            EthChainId chainId = web3j.ethChainId().send();
            return getNetworkById(chainId.getChainId().longValue());
        } catch (Exception e) {
            log.warn("Failed to get current network, using configured chainId: {}", 
                web3Config.getChainId());
            return getNetworkById(web3Config.getChainId());
        }
    }
    
    public boolean isNetworkSupported(Long chainId) {
        return SUPPORTED_NETWORKS.containsKey(chainId);
    }
    
    public String formatAddress(String address) {
        if (address == null || address.length() < 10) {
            return address;
        }
        return address.substring(0, 6) + "..." + address.substring(address.length() - 4);
    }
    
    public boolean isValidAddress(String address) {
        return address != null && address.matches("^0x[a-fA-F0-9]{40}$");
    }
}
