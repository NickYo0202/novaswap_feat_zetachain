package com.novaswap.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Network {
    private Long chainId;
    private String name;
    private String symbol;
    private String rpcUrl;
    private String explorerUrl;
    private String multicallAddress;
    private String routerAddress;
    private String wethAddress;
    private boolean isTestnet;
    
    public static Network ethereum() {
        return new Network(1L, "Ethereum", "ETH", 
            "https://mainnet.infura.io/v3/YOUR_KEY",
            "https://etherscan.io", 
            "0xcA11bde05977b3631167028862bE2a173976CA11",
            "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D",
            "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2",
            false);
    }
    
    public static Network arbitrum() {
        return new Network(42161L, "Arbitrum One", "ETH",
            "https://arb1.arbitrum.io/rpc",
            "https://arbiscan.io",
            "0xcA11bde05977b3631167028862bE2a173976CA11",
            "0x1b02dA8Cb0d097eB8D57A175b88c7D8b47997506",
            "0x82aF49447D8a07e3bd95BD0d56f35241523fBab1",
            false);
    }
    
    public static Network optimism() {
        return new Network(10L, "Optimism", "ETH",
            "https://mainnet.optimism.io",
            "https://optimistic.etherscan.io",
            "0xcA11bde05977b3631167028862bE2a173976CA11",
            "0x4A7b5Da61326A6379179b40d00F57E5bbDC962c2",
            "0x4200000000000000000000000000000000000006",
            false);
    }
    
    public static Network polygon() {
        return new Network(137L, "Polygon", "MATIC",
            "https://polygon-rpc.com",
            "https://polygonscan.com",
            "0xcA11bde05977b3631167028862bE2a173976CA11",
            "0xa5E0829CaCEd8fFDD4De3c43696c57F7D7A678ff",
            "0x0d500B1d8E8eF31E21C99d1Db9A6444d3ADf1270",
            false);
    }
    
    public static Network bsc() {
        return new Network(56L, "BNB Chain", "BNB",
            "https://bsc-dataseed.binance.org",
            "https://bscscan.com",
            "0xcA11bde05977b3631167028862bE2a173976CA11",
            "0x10ED43C718714eb63d5aA57B78B54704E256024E",
            "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c",
            false);
    }
    
    public static Network avalanche() {
        return new Network(43114L, "Avalanche C-Chain", "AVAX",
            "https://api.avax.network/ext/bc/C/rpc",
            "https://snowtrace.io",
            "0xcA11bde05977b3631167028862bE2a173976CA11",
            "0x60aE616a2155Ee3d9A68541Ba4544862310933d4",
            "0xB31f66AA3C1e785363F0875A1B74E27b85FD66c7",
            false);
    }
    
    public static Network zetaChain() {
        return new Network(7000L, "ZetaChain", "ZETA",
            "https://zetachain-evm.blockpi.network/v1/rpc/public",
            "https://explorer.zetachain.com",
            "0xcA11bde05977b3631167028862bE2a173976CA11",
            "0x2ca7d64A7EFE2D62A725E2B35Cf7230D6677FfEe",
            "0x5F0b1a82749cb4E2278EC87F8BF6B618dC71a8bf",
            false);
    }
}
