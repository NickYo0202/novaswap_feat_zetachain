package com.novaswap.api.controller;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.novaswap.api.dto.ApproveRequest;
import com.novaswap.api.dto.LiquidityAddRequest;
import com.novaswap.api.dto.LiquidityRemoveRequest;
import com.novaswap.api.dto.SwapRequest;
import com.novaswap.contract.AllowanceService;
import com.novaswap.contract.RouterService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/dex")
@Validated
public class DexController {

    private static final long DEFAULT_DEADLINE_SECONDS = 20 * 60; // 20 minutes

    private final RouterService routerService;
    private final AllowanceService allowanceService;

    public DexController(RouterService routerService, AllowanceService allowanceService) {
        this.routerService = routerService;
        this.allowanceService = allowanceService;
    }

    @PostMapping("/swap")
    public ResponseEntity<Map<String, String>> swap(@Valid @RequestBody SwapRequest req) {
        BigInteger deadline = resolveDeadline(req.getDeadline());
        return routerService.swapExactTokensForTokens(
                        req.getAmountIn(),
                        req.getAmountOutMin(),
                        req.getPath(),
                        req.getTo(),
                        deadline)
                .thenApply(tx -> ResponseEntity.ok(Map.of("txHash", tx)))
                .join();
    }

    @PostMapping("/add-liquidity")
    public ResponseEntity<Map<String, String>> addLiquidity(@Valid @RequestBody LiquidityAddRequest req) {
        BigInteger deadline = resolveDeadline(req.getDeadline());
        return routerService.addLiquidity(
                        req.getTokenA(),
                        req.getTokenB(),
                        req.getAmountADesired(),
                        req.getAmountBDesired(),
                        req.getAmountAMin(),
                        req.getAmountBMin(),
                        req.getTo(),
                        deadline)
                .thenApply(tx -> ResponseEntity.ok(Map.of("txHash", tx)))
                .join();
    }

    @PostMapping("/remove-liquidity")
    public ResponseEntity<Map<String, String>> removeLiquidity(@Valid @RequestBody LiquidityRemoveRequest req) {
        BigInteger deadline = resolveDeadline(req.getDeadline());
        return routerService.removeLiquidity(
                        req.getTokenA(),
                        req.getTokenB(),
                        req.getLiquidity(),
                        req.getAmountAMin(),
                        req.getAmountBMin(),
                        req.getTo(),
                        deadline)
                .thenApply(tx -> ResponseEntity.ok(Map.of("txHash", tx)))
                .join();
    }

    @PostMapping("/approve")
    public ResponseEntity<Map<String, String>> approve(@Valid @RequestBody ApproveRequest req) {
        return allowanceService.approve(req.getToken(), req.getSpender(), req.getAmount())
                .thenApply(tx -> ResponseEntity.ok(Map.of("txHash", tx)))
                .join();
    }

    private BigInteger resolveDeadline(Long deadline) {
        long ts = (deadline != null && deadline > 0) ? deadline : Instant.now().getEpochSecond() + DEFAULT_DEADLINE_SECONDS;
        return BigInteger.valueOf(ts);
    }
}
