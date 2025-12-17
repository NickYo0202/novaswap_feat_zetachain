# NovaSwap 链上交互服务 API 文档

## 概述
本模块实现了NovaSwap的链上交互服务（Node/Web3网关），提供多链支持、钱包管理、余额查询、授权管理和路由搜索等核心功能。

## 功能模块

### 1. 网络管理 (NetworkController)
**基础路径:** `/api/network`

#### 1.1 获取支持的网络列表
```http
GET /api/network/supported
```
**响应示例:**
```json
[
  {
    "chainId": 1,
    "name": "Ethereum",
    "symbol": "ETH",
    "rpcUrl": "https://mainnet.infura.io/v3/YOUR_KEY",
    "explorerUrl": "https://etherscan.io",
    "multicallAddress": "0xcA11bde05977b3631167028862bE2a173976CA11",
    "routerAddress": "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D",
    "wethAddress": "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2",
    "testnet": false
  }
]
```

#### 1.2 获取当前网络
```http
GET /api/network/current
```

#### 1.3 检查网络支持
```http
GET /api/network/check/{chainId}
```

#### 1.4 格式化地址（脱敏显示）
```http
GET /api/network/format-address?address=0x1234...5678
```
**响应:**
```json
{
  "original": "0x1234567890123456789012345678901234567890",
  "formatted": "0x1234...7890",
  "valid": "true"
}
```

#### 1.5 验证地址格式
```http
GET /api/network/validate-address?address=0x1234...
```

---

### 2. 余额查询 (BalanceController)
**基础路径:** `/api/balance`

#### 2.1 获取ETH余额
```http
GET /api/balance/eth/{address}
```
**响应:**
```json
{
  "address": "0x...",
  "balance": "1000000000000000000",
  "balanceInEth": "1"
}
```

#### 2.2 获取单个ERC20代币余额
```http
GET /api/balance/token?tokenAddress=0x...&userAddress=0x...
```
**响应:**
```json
{
  "tokenAddress": "0x...",
  "tokenSymbol": "USDC",
  "tokenName": "USD Coin",
  "decimals": 6,
  "balance": "1000000",
  "formattedBalance": "1.0"
}
```

#### 2.3 批量获取多个代币余额
```http
POST /api/balance/multiple
Content-Type: application/json

{
  "userAddress": "0x...",
  "tokenAddresses": ["0x...", "0x..."],
  "chainId": 1
}
```

---

### 3. 授权管理 (AllowanceController)
**基础路径:** `/api/allowance`

#### 3.1 检查授权额度
```http
GET /api/allowance/check?tokenAddress=0x...&owner=0x...&spender=0x...
```
**响应:**
```json
{
  "tokenAddress": "0x...",
  "owner": "0x...",
  "spender": "0x...",
  "allowance": "1000000000000000000"
}
```

#### 3.2 检查是否需要授权
```http
POST /api/allowance/needs-approval
Content-Type: application/json

{
  "tokenAddress": "0x...",
  "owner": "0x...",
  "spender": "0x...",
  "amount": "1000000000000000000"
}
```
**响应:**
```json
{
  "needsApproval": true,
  "currentAllowance": "0",
  "requiredAmount": "1000000000000000000",
  "approveCalldata": "0x095ea7b3..."
}
```

#### 3.3 生成授权交易数据
```http
POST /api/allowance/build-approve?spender=0x...&infinite=true
```

#### 3.4 检查双币授权状态（用于添加流动性）
```http
POST /api/allowance/check-dual?token0=0x...&token1=0x...&owner=0x...&spender=0x...&amount0=1000000&amount1=2000000
```
**响应:**
```json
{
  "0xToken0Address": false,
  "0xToken1Address": true
}
```

---

### 4. 路由搜索 (RouteController)
**基础路径:** `/api/route`

#### 4.1 搜索最优路由
```http
POST /api/route/search
Content-Type: application/json

{
  "tokenIn": "0x...",
  "tokenOut": "0x...",
  "amountIn": "1000000000000000000",
  "slippageTolerance": 0.005,
  "intermediateTokens": ["0xWETH", "0xUSDC"]
}
```
**响应:**
```json
{
  "path": ["0xTokenIn", "0xWETH", "0xTokenOut"],
  "amountOut": "2000000000000000000",
  "minAmountOut": "1990000000000000000",
  "priceImpact": "0.5",
  "reserves": ["1000000000", "2000000000", "3000000000", "4000000000"],
  "direct": false,
  "hops": 2
}
```

#### 4.2 获取兑换预估
```http
POST /api/route/quote
Content-Type: application/json

{
  "tokenIn": "0x...",
  "tokenOut": "0x...",
  "amountIn": "1000000000000000000",
  "slippageTolerance": 0.005
}
```
**响应:**
```json
{
  "path": ["0xTokenIn", "0xTokenOut"],
  "amountOut": "2000000000000000000",
  "minAmountOut": "1990000000000000000",
  "priceImpact": "0.35%",
  "isDirect": true,
  "hops": 1,
  "slippageTolerance": "0.5%",
  "highPriceImpact": false
}
```

---

## 支持的网络

| 网络 | Chain ID | 符号 |
|------|----------|------|
| Ethereum | 1 | ETH |
| Arbitrum One | 42161 | ETH |
| Optimism | 10 | ETH |
| Polygon | 137 | MATIC |
| BNB Chain | 56 | BNB |
| Avalanche C-Chain | 43114 | AVAX |
| ZetaChain | 7000 | ZETA |

---

## 错误处理

所有API统一使用以下错误格式：

```json
{
  "error": "错误描述",
  "type": "ERROR_TYPE",
  "details": "详细信息"
}
```

### 错误类型

- `INVALID_ARGUMENT` - 参数错误
- `INSUFFICIENT_LIQUIDITY` - 流动性不足
- `NO_ROUTE_FOUND` - 无可用路由
- `INSUFFICIENT_BALANCE` - 余额不足
- `INSUFFICIENT_ALLOWANCE` - 授权额度不足
- `TRANSACTION_EXPIRED` - 交易已过期
- `RUNTIME_ERROR` - 运行时错误
- `INTERNAL_ERROR` - 内部错误

---

## 核心算法

### 1. 兑换输出计算（恒定乘积公式）
```
amountOut = (amountIn * 997 * reserveOut) / (reserveIn * 1000 + amountIn * 997)
```
- 手续费：0.3% (997/1000)
- 基于 x * y = k 恒定乘积模型

### 2. 价格影响计算
```
priceImpact = |(priceAfter - priceBefore) / priceBefore| * 100%
```

### 3. 滑点保护
```
minAmountOut = amountOut * (1 - slippageTolerance)
```

---

## 配置说明

在 `application.yml` 中配置各网络参数：

```yaml
novaswap:
  networks:
    ethereum:
      chainId: 1
      rpcUrl: https://mainnet.infura.io/v3/YOUR_KEY
      router: "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D"
      weth: "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
      multicall: "0xcA11bde05977b3631167028862bE2a173976CA11"
```

---

## 使用示例

### 完整兑换流程

1. **检查授权**
```bash
curl -X POST http://localhost:8089/api/allowance/needs-approval \
  -H "Content-Type: application/json" \
  -d '{"tokenAddress":"0x...","owner":"0x...","spender":"0xRouter","amount":"1000000"}'
```

2. **如果需要授权，发起approve交易**
```bash
curl -X POST http://localhost:8089/api/allowance/build-approve?spender=0xRouter&infinite=true
```

3. **搜索最优路由**
```bash
curl -X POST http://localhost:8089/api/route/search \
  -H "Content-Type: application/json" \
  -d '{"tokenIn":"0xUSDC","tokenOut":"0xWETH","amountIn":"1000000","slippageTolerance":0.005}'
```

4. **执行兑换**（使用DexController中的swap接口）

---

## Swagger文档

启动应用后访问：
```
http://localhost:8089/swagger-ui.html
```

查看完整的API文档和在线测试界面。

---

## 技术栈

- Spring Boot 3.3.5
- Web3j 4.12.1
- Java 21
- Lombok
- SpringDoc OpenAPI 3

---

## 性能优化

1. **Multicall聚合查询** - 减少RPC调用次数
2. **缓存策略** - 高频数据缓存（待实现）
3. **异步处理** - 批量查询使用并行流
4. **连接池** - Web3j HTTP连接复用

---

## 安全考虑

1. ✅ 输入验证 - 使用Jakarta Validation
2. ✅ 地址格式验证 - 正则表达式校验
3. ✅ 授权最小化 - 支持精确授权和无限授权
4. ✅ 错误信息脱敏 - 不暴露敏感信息
5. ⚠️ 速率限制 - 待实现
6. ⚠️ CORS配置 - 需根据前端域名配置

---

## 待实现功能

基于 `backend_tasks.md` 的需求，以下功能待后续开发：

- [ ] Multicall批量查询优化
- [ ] 价格与滑点计算增强
- [ ] 跨链交易编排（ZetaChain集成）
- [ ] 事件索引与数据层
- [ ] 缓存层（Redis）
- [ ] RPC节点故障转移
- [ ] 健康检查与限流
- [ ] 跨链桥接逻辑
- [ ] Gas费用代付
- [ ] 全链资产聚合

---

## 联系与支持

有问题请查看项目文档或提交issue。
