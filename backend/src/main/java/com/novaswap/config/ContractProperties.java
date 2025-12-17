package com.novaswap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "novaswap.contract")
public class ContractProperties {
    private String router;
    private String weth;
    private String multicall;

    public String getRouter() {
        return router;
    }

    public void setRouter(String router) {
        this.router = router;
    }

    public String getWeth() {
        return weth;
    }

    public void setWeth(String weth) {
        this.weth = weth;
    }

    public String getMulticall() {
        return multicall;
    }

    public void setMulticall(String multicall) {
        this.multicall = multicall;
    }
}
