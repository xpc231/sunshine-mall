-- 令牌桶 Lua 脚本（分布式限流）
-- KEYS[1] : 桶的Redis Key
-- ARGV[1] : capacity 桶容量
-- ARGV[2] : refill_rate 每秒填充的令牌数
-- ARGV[3] : requested 本次请求需要的令牌数
-- ARGV[4] : now 当前时间戳（毫秒）

local bucket_key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local requested = tonumber(ARGV[3])
local now = tonumber(ARGV[4])

-- 获取当前桶中的令牌数和上次补充时间
local tokens = redis.call('HGET', bucket_key, 'tokens')
local last_refill = redis.call('HGET', bucket_key, 'last_refill_ts')

-- 如果是第一次访问，初始化令牌桶
if tokens == false then
  tokens = capacity
  last_refill = now
else
  tokens = tonumber(tokens) -- 转换为数字类型
  last_refill = tonumber(last_refill)
end

-- 根据时间间隔补充令牌
local elapsed_ms = now - last_refill
if elapsed_ms > 0 then
  -- 基于经过的时间和令牌桶的填充速率来确定需要补充的令牌数
  local add = math.floor((elapsed_ms / 1000.0) * refill_rate)

  if add > 0 then
    tokens = math.min(capacity, tokens + add)
    last_refill = now
  end
end

-- 检查是否有足够的令牌处理请求
local allowed = 0
if tokens >= requested then
  tokens = tokens - requested
  allowed = 1
end

-- 更新令牌桶状态并设置过期时间
redis.call('HSET', bucket_key, 'tokens', tokens, 'last_refill_ts', last_refill)
redis.call('EXPIRE', bucket_key, 3600)

return {allowed}
