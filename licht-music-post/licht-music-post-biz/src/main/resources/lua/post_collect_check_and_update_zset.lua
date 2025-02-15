local key = KEYS[1] -- Redis Key
local postId = ARGV[1] -- 帖子ID
local timestamp = ARGV[2] -- 时间戳

-- 使用 EXISTS 命令检查 ZSET 帖子收藏列表是否存在
local exists = redis.call('EXISTS', key)
if exists == 0 then
    return -1
end

-- 获取帖子收藏列表大小
local size = redis.call('ZCARD', key)

-- 若已经收藏了 300 篇帖子，则移除最早收藏的那篇
if size >= 300 then
    redis.call('ZPOPMIN', key)
end

-- 添加新的帖子收藏关系
redis.call('ZADD', key, timestamp, postId)
return 0
