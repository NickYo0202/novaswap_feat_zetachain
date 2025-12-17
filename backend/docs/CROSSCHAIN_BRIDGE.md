# 跨链桥接功能文档

## 概述

本项目已完成跨链桥接逻辑的后端代码开发，支持通过ZetaChain实现多链之间的资产跨链转移和交易。

## 功能特性

### 1. 跨链路由搜索
- **直接桥接路由**：支持原生桥接资产直接跨链
- **稳定币中转路由**：通过USDC作为中转资产实现跨链
- **智能路由选择**：
  - FASTEST（最快路径）：优先时间，最快完成
  - CHEAPEST（最省钱路径）：优先费用，最低成本
  - BALANCED（平衡路径）：时间与费用综合最优

### 2. 费用计算
- **源链Gas费**：Swap和Bridge操作的gas消耗
- **桥接费**：ZetaChain桥接基础费用（2M-5M wei）
- **目标链Gas费**：目标链执行和接收的gas费用
- **服务费**：0.05%的平台服务费
- **第三方费用**：桥接协议收取的费用
- **费用支付方式**：支持任意代币支付（USD等值转换）

### 3. 交易状态追踪
- **实时状态更新**：8种交易状态全程追踪
  - PENDING_SOURCE_CONFIRMATION：等待源链确认
  - SOURCE_CONFIRMED：源链已确认
  - BRIDGE_INITIATED：桥接已发起
  - BRIDGE_IN_PROGRESS：桥接进行中
  - TARGET_EXECUTING：目标链执行中
  - COMPLETED：已完成
  - PARTIALLY_COMPLETED：部分完成
  - FAILED/REFUNDED：失败或已退款
- **状态历史记录**：完整的状态变更日志
- **区块浏览器链接**：自动生成各链的区块浏览器链接
- **自动监控**：每30秒检查一次待处理交易

### 4. 安全机制
- **断路器（Circuit Breaker）**：异常时自动暂停桥接
- **每日限额**：每条链每日最多转出100k代币
- **重试机制**：失败交易支持最多3次重试
- **退款机制**：失败时自动退款至源链

### 5. ZetaChain集成
- **桥接路径支持**：
  - Ethereum ↔ BSC
  - Ethereum ↔ Polygon
  - BSC ↔ Polygon
- **桥接时间**：3-5分钟完成跨链
- **消息验证**：签名验证确保安全性

## 技术架构

### 模型层（Model）
1. **CrossChainRoute**：跨链路由模型
   - RouteStep：路由步骤（SWAP/BRIDGE/RECEIVE）
   - FeeBreakdown：费用分解
   - RouteType：路由类型枚举
   - StepType：步骤类型枚举

2. **CrossChainTransaction**：跨链交易模型
   - TransactionStatus：交易状态枚举
   - StatusHistory：状态历史记录
   - 支持双链交易哈希和桥接消息ID

3. **BridgeConfig**：桥接配置模型
   - ChainPair：支持的链对
   - CircuitBreakerStatus：断路器状态
   - 每日限额和平均时间配置

### 服务层（Service）
1. **ZetaChainService**：ZetaChain集成服务
   - 桥接路径验证
   - 费用计算（2M-5M wei基础费）
   - 时间估算（180-300秒）
   - 跨链消息构建和发送
   - 消息状态查询
   - 签名验证
   - 断路器和限额管理

2. **CrossChainRouteService**：跨链路由搜索服务
   - 直接桥接路由
   - 稳定币中转路由
   - 多跳路由优化
   - 路由类型排序（FASTEST/CHEAPEST/BALANCED）

3. **CrossChainFeeService**：跨链费用计算服务
   - 源链/目标链gas费计算
   - 桥接费和服务费计算
   - 任意代币支付换算
   - 中继服务费计算
   - 退款金额计算

4. **CrossChainTransactionService**：交易状态追踪服务
   - 交易记录创建和更新
   - 状态历史记录
   - 自动监控（30秒/次）
   - 用户交易历史查询
   - 过期交易清理（30天）

5. **CrossChainBridgeService**：跨链桥接主服务
   - 完整跨链流程编排
   - 源链swap → 桥接 → 目标链swap
   - 失败处理和退款
   - 重试机制
   - 异步执行

### 控制器层（Controller）
**CrossChainController**：提供REST API接口
- `POST /api/crosschain/routes`：搜索跨链路由
- `POST /api/crosschain/estimate-fee`：估算跨链费用
- `POST /api/crosschain/swap`：执行跨链swap
- `GET /api/crosschain/transaction/{transactionId}`：查询交易状态
- `GET /api/crosschain/transactions/user/{userAddress}`：查询用户交易历史
- `POST /api/crosschain/transaction/{transactionId}/retry`：重试失败交易
- `GET /api/crosschain/supported-paths`：获取支持的桥接路径

### DTO层（Data Transfer Object）
1. **CrossChainRouteRequest/Response**：路由搜索请求/响应
2. **CrossChainSwapRequest/Response**：swap执行请求/响应
3. **CrossChainTransactionStatusResponse**：交易状态响应
4. **CrossChainFeeEstimateResponse**：费用估算响应
5. **SupportedBridgePathsResponse**：支持的桥接路径响应

## API使用示例

### 1. 搜索跨链路由
```http
POST /api/crosschain/routes
Content-Type: application/json

{
  "sourceChainId": 1,
  "targetChainId": 56,
  "sourceTokenAddress": "0x...",
  "targetTokenAddress": "0x...",
  "amountIn": "1000000000000000000",
  "routeType": "BALANCED",
  "slippagePercent": 0.5
}
```

### 2. 执行跨链swap
```http
POST /api/crosschain/swap
Content-Type: application/json

{
  "sourceChainId": 1,
  "targetChainId": 56,
  "sourceTokenAddress": "0x...",
  "targetTokenAddress": "0x...",
  "amountIn": "1000000000000000000",
  "userAddress": "0x...",
  "slippagePercent": 0.5
}
```

### 3. 查询交易状态
```http
GET /api/crosschain/transaction/TX-1234567890ABCDEF
```

## 性能指标

- **跨链时间**：3-5分钟
- **支持的链**：Ethereum、BSC、Polygon
- **桥接费用**：2M-5M wei（取决于链对）
- **服务费率**：0.05%
- **每日限额**：100k代币/链
- **重试次数**：最多3次
- **监控频率**：30秒/次

## 后续优化方向

1. **更多桥接协议支持**
   - LayerZero集成
   - Wormhole集成
   - Stargate集成

2. **更多链支持**
   - Arbitrum
   - Optimism  
   - Avalanche

3. **高级功能**
   - 跨链闪电贷
   - 跨链聚合器
   - 跨链限价单
   - MEV保护

4. **性能优化**
   - 数据库持久化（替代内存存储）
   - Redis缓存加速
   - WebSocket实时推送

5. **安全增强**
   - 多签钱包支持
   - 时间锁保护
   - 异常检测系统

## 文件清单

### 模型类（3个文件）
- `com.novaswap.model.crosschain.CrossChainRoute`
- `com.novaswap.model.crosschain.CrossChainTransaction`
- `com.novaswap.model.crosschain.BridgeConfig`

### 服务类（5个文件）
- `com.novaswap.service.crosschain.ZetaChainService`
- `com.novaswap.service.crosschain.CrossChainRouteService`
- `com.novaswap.service.crosschain.CrossChainFeeService`
- `com.novaswap.service.crosschain.CrossChainTransactionService`
- `com.novaswap.service.crosschain.CrossChainBridgeService`

### DTO类（7个文件）
- `com.novaswap.api.dto.crosschain.CrossChainRouteRequest`
- `com.novaswap.api.dto.crosschain.CrossChainRouteResponse`
- `com.novaswap.api.dto.crosschain.CrossChainSwapRequest`
- `com.novaswap.api.dto.crosschain.CrossChainSwapResponse`
- `com.novaswap.api.dto.crosschain.CrossChainTransactionStatusResponse`
- `com.novaswap.api.dto.crosschain.CrossChainFeeEstimateResponse`
- `com.novaswap.api.dto.crosschain.SupportedBridgePathsResponse`

### 控制器类（1个文件）
- `com.novaswap.api.controller.CrossChainController`

**总计：16个新增文件，~2500行代码**

## 开发完成情况

✅ 跨链桥接模型类
✅ ZetaChain集成服务
✅ 跨链路径搜索服务
✅ 跨链费用计算服务
✅ 跨链交易状态追踪服务
✅ 跨链桥接主服务
✅ 跨链DTOs
✅ 跨链REST控制器
✅ 所有编译错误已修复
✅ 代码规范符合项目要求

**状态：跨链桥接功能开发完成 ✅**
