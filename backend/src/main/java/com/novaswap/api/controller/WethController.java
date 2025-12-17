package com.novaswap.api.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.novaswap.api.dto.UnwrapRequest;
import com.novaswap.api.dto.WrapRequest;
import com.novaswap.contract.WethService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/weth")
@Validated
public class WethController {

    private final WethService wethService;

    public WethController(WethService wethService) {
        this.wethService = wethService;
    }

    @PostMapping("/wrap")
    public ResponseEntity<Map<String, String>> wrap(@Valid @RequestBody WrapRequest req) {
        return wethService.wrap(req.getAmountWei())
                .thenApply(tx -> ResponseEntity.ok(Map.of("txHash", tx)))
                .join();
    }

    @PostMapping("/unwrap")
    public ResponseEntity<Map<String, String>> unwrap(@Valid @RequestBody UnwrapRequest req) {
        return wethService.unwrap(req.getAmountWei())
                .thenApply(tx -> ResponseEntity.ok(Map.of("txHash", tx)))
                .join();
    }
}
