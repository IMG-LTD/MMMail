# Billing Webhook Signature

MMMail 主仓只接收外部计费状态，不实现支付网关，也不签发真实付款成功。

## Header

计费 webhook 必须带：

```text
X-MMMail-Billing-Signature: v1=<hex-hmac-sha256>
```

## Signing Input

`v1` 签名使用 HMAC-SHA256。输入按换行拼接：

```text
signatureVersion
eventId
orgId
plan
status
occurredAt
```

服务端只接受 5 分钟时间窗内的事件。签名失败、密钥缺失、版本不支持或时间窗失败都必须显式失败，不允许 fallback 为成功。

自托管部署通过 `MMMAIL_BILLING_WEBHOOK_SECRET` 配置共享密钥。模板中只能保留 `replace-with-32-plus-char-random-secret`，真实密钥不得提交到源码。

## Idempotency

`eventId` 是幂等键。已处理事件再次到达时只返回 duplicate 结果，不重复更新 `org_subscription_state`。
