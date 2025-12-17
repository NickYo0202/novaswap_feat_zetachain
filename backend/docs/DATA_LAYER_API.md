# NovaSwap 索引与数据层 API 文档

## 新增功能模块

本次更新实现了完整的索引与数据层功能，包括事件监听、数据聚合、统计分析、价格历史和缓存机制。

---

## 1. 池统计 API (PoolStatsController)

**基础路径:** `/api/stats`

### 1.1 获取池统计信息
```http
GET /api/stats/pool/{pairAddress}
```

**响应示例:**
```json
{
  "pairAddress": "0x...",
  "token0": "0x...",
  "token1": "0x...",
  "token0Symbol": "USDC",
  "token1Symbol": "WETH",
  "reserve0": "1000000000",
  "reserve1": "500000000000000000",
  "tvlUsd": "1000000.00",
  "volume24hUsd": "100000.00",
  "volume24hToken0": "50000000",
  "volume24hToken1": "25000000000000000",
  "fees24hUsd": "300.00",
  "apy": "109.5",
  "txCount24h": 150,
  "lpCount": 50,
  "chainId": 1
}
```

### 1.2 获取所有池统计
```http
GET /api/stats/pools
```

### 1.3 获取热门池（按TVL排序）
```http
GET /api/stats/pools/top-tvl?limit=10
```

### 1.4 获取热门池（按交易量排序）
```http
GET /api/stats/pools/top-volume?limit=10
```

### 1.5 获取池APY
```http
GET /api/stats/pool/{pairAddress}/apy
```

**响应:**
```json
{
  "pairAddress": "0x...",
  "apy": "109.5",
  "tvl": "1000000.00",
  "fees24h": "300.00",
  "volume24h": "100000.00"
}
```

### 1.6 刷新池统计
```http
POST /api/stats/pool/{pairAddress}/refresh
```

### 1.7 清除统计缓存
```http
POST /api/stats/cache/clear
```

---

## 2. 价格历史 API (PriceHistoryController)

**基础路径:** `/api/price`

### 2.1 获取价格历史（K线数据）
```http
POST /api/price/history
Content-Type: application/json

{
  "pairAddress": "0x...",
  "interval": "1h",
  "startTime": 1702713600,
  "endTime": 1702800000,
  "limit": 100
}
```

**支持的时间间隔:**
- `1m` - 1分钟
- `5m` - 5分钟
- `15m` - 15分钟
- `1h` - 1小时
- `4h` - 4小时
- `1d` - 1天
- `1w` - 1周

**响应示例:**
```json
[
  {
    "pairAddress": "0x...",
    "timestamp": "2025-12-16T10:00:00Z",
    "open": "1000.50",
    "high": "1020.00",
    "low": "995.00",
    "close": "1010.25",
    "volume": "25000.00",
    "interval": "1h"
  }
]
```

### 2.2 获取支持的时间间隔
```http
GET /api/price/intervals
```

**响应:**
```json
{
  "intervals": ["1m", "5m", "15m", "1h", "4h", "1d", "1w"],
  "default": "1h"
}
```

### 2.3 获取最新价格
```http
GET /api/price/{pairAddress}/current
```

---

## 3. 事件查询 API (EventsController)

**基础路径:** `/api/events`

### 3.1 获取Swap事件
```http
GET /api/events/swap?pairAddress=0x...&fromBlock=1000000&toBlock=1001000
```

**响应:**
```json
[
  {
    "txHash": "0x...",
    "pairAddress": "0x...",
    "sender": "0x...",
    "to": "0x...",
    "amount0In": "1000000",
    "amount1In": "0",
    "amount0Out": "0",
    "amount1Out": "500000000000000000",
    "blockNumber": "1000500",
    "timestamp": "2025-12-16T10:30:00Z",
    "chainId": 1
  }
]
```

### 3.2 获取Mint事件（添加流动性）
```http
GET /api/events/mint?pairAddress=0x...&fromBlock=1000000&toBlock=1001000
```

### 3.3 获取Burn事件（移除流动性）
```http
GET /api/events/burn?pairAddress=0x...&fromBlock=1000000&toBlock=1001000
```

### 3.4 获取当前区块号
```http
GET /api/events/block/current
```

**响应:**
```json
{
  "blockNumber": "18500000",
  "timestamp": 1702800000
}
```

---

## 4. 健康检查 API (HealthController)

**基础路径:** `/api`

### 4.1 健康检查
```http
GET /api/health
```

**响应:**
```json
{
  "status": "UP",
  "timestamp": 1702800000,
  "service": "NovaSwap Backend",
  "version": "0.1.0",
  "web3": "connected"
}
```

### 4.2 服务信息
```http
GET /api/info
```

**响应:**
```json
{
  "name": "NovaSwap Backend API",
  "version": "0.1.0-SNAPSHOT",
  "description": "DEX Backend Services with Multi-chain Support",
  "features": {
    "multichain": true,
    "swap": true,
    "liquidity": true,
    "statistics": true,
    "events": true,
    "priceHistory": true,
    "cache": true
  }
}
```

---

## 核心算法实现

### 1. TVL计算
```java
TVL = (reserve0 * price0USD + reserve1 * price1USD)
```

### 2. APY计算
```java
APY = (fees24h * 365 / TVL) * 100%
```

### 3. 24h手续费计算
```java
fees24h = volume24h * 0.003  // 0.3%手续费
```

### 4. 价格计算
```java
price = reserve1 / reserve0
```

---

## 缓存策略

### 缓存配置
使用Spring Cache + ConcurrentHashMap实现内存缓存：

1. **poolStats** - 池统计数据缓存（10秒过期）
2. **tokenBalances** - 代币余额缓存
3. **priceHistory** - 价格历史缓存
4. **routeCache** - 路由缓存

### 定时更新
- 池统计数据：每10秒自动更新
- 缓存清理：每小时执行一次

### 缓存替换
生产环境可替换为Redis：
```yaml
spring:
  redis:
    host: localhost
    port: 6379
  cache:
    type: redis
```

---

## 事件监听机制

### Swap事件
```solidity
event Swap(
    address indexed sender,
    uint amount0In,
    uint amount1In,
    uint amount0Out,
    uint amount1Out,
    address indexed to
);
```

### Mint事件
```solidity
event Mint(
    address indexed sender,
    uint amount0,
    uint amount1
);
```

### Burn事件
```solidity
event Burn(
    address indexed sender,
    uint amount0,
    uint amount1,
    address indexed to
);
```

---

## 数据模型

### PoolStats（池统计）
```typescript
{
  pairAddress: string
  token0: string
  token1: string
  reserve0: bigint
  reserve1: bigint
  tvlUsd: decimal
  volume24hUsd: decimal
  fees24hUsd: decimal
  apy: decimal
  txCount24h: long
  lpCount: long
}
```

### PricePoint（价格点）
```typescript
{
  pairAddress: string
  timestamp: instant
  open: decimal
  high: decimal
  low: decimal
  close: decimal
  volume: decimal
  interval: string
}
```

### SwapEvent（兑换事件）
```typescript
{
  txHash: string
  pairAddress: string
  sender: string
  to: string
  amount0In: bigint
  amount1In: bigint
  amount0Out: bigint
  amount1Out: bigint
  blockNumber: bigint
  timestamp: instant
}
```

---

## 使用示例

### 1. 查询池统计和APY
```bash
# 获取池统计
curl http://localhost:8089/api/stats/pool/0xPairAddress

# 获取热门池
curl http://localhost:8089/api/stats/pools/top-tvl?limit=5

# 获取APY
curl http://localhost:8089/api/stats/pool/0xPairAddress/apy
```

### 2. 获取价格历史
```bash
curl -X POST http://localhost:8089/api/price/history \
  -H "Content-Type: application/json" \
  -d '{
    "pairAddress": "0xPairAddress",
    "interval": "1h",
    "startTime": 1702713600,
    "endTime": 1702800000,
    "limit": 24
  }'
```

### 3. 查询链上事件
```bash
# 获取Swap事件
curl "http://localhost:8089/api/events/swap?pairAddress=0x...&fromBlock=1000000&toBlock=1001000"

# 获取当前区块
curl http://localhost:8089/api/events/block/current
```

### 4. 健康检查
```bash
curl http://localhost:8089/api/health
```

---

## 性能优化

1. **缓存层** - 减少重复计算和RPC调用
2. **定时更新** - 后台异步更新统计数据
3. **批量查询** - 事件批量获取减少网络开销
4. **索引优化** - 使用区块号范围查询

---

## 待实现功能

- [ ] 持久化存储（数据库集成）
- [ ] Redis分布式缓存
- [ ] WebSocket实时推送
- [ ] 事件回放和重新索引
- [ ] 跨链事件聚合
- [ ] 更详细的事件解析
- [ ] 价格预言机集成
- [ ] 历史数据归档

---

## API完整列表

### 网络管理
- `GET /api/network/supported`
- `GET /api/network/current`
- `GET /api/network/check/{chainId}`
- `GET /api/network/format-address`
- `GET /api/network/validate-address`

### 余额查询
- `GET /api/balance/eth/{address}`
- `GET /api/balance/token`
- `POST /api/balance/multiple`

### 授权管理
- `GET /api/allowance/check`
- `POST /api/allowance/needs-approval`
- `POST /api/allowance/build-approve`
- `POST /api/allowance/check-dual`

### 路由搜索
- `POST /api/route/search`
- `POST /api/route/quote`

### 池统计 ⭐新增
- `GET /api/stats/pool/{pairAddress}`
- `GET /api/stats/pools`
- `GET /api/stats/pools/top-tvl`
- `GET /api/stats/pools/top-volume`
- `GET /api/stats/pool/{pairAddress}/apy`
- `POST /api/stats/pool/{pairAddress}/refresh`
- `POST /api/stats/cache/clear`

### 价格历史 ⭐新增
- `POST /api/price/history`
- `GET /api/price/intervals`
- `GET /api/price/{pairAddress}/current`

### 事件查询 ⭐新增
- `GET /api/events/swap`
- `GET /api/events/mint`
- `GET /api/events/burn`
- `GET /api/events/block/current`

### 健康检查 ⭐新增
- `GET /api/health`
- `GET /api/info`

---

**Swagger文档:** `http://localhost:8089/swagger-ui.html`

**开发完成时间:** 2025-12-16  
**新增代码行数:** 约1500+行
