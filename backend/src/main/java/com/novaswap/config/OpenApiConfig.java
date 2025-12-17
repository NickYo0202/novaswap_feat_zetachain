package com.novaswap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI novaswapOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("NovaSwap Backend API")
                        .description("DEX Router / WETH / Liquidity operations")
                        .version("v1"));
    }
}
