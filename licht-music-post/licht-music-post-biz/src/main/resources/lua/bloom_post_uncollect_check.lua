local key = KEYS[1] -- 操作的 Redis Key
local postId = ARGV[1] -- 帖子ID

-- 使用 EXISTS 命令检查布隆过滤器是否存在
local exists = redis.call('EXISTS', key)
if exists == 0 then
    return -1
end

-- 校验该篇帖子是否被收藏过(1 表示已经收藏，0 表示未收藏)
return redis.call('BF.EXISTS', key, postId)
