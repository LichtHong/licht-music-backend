-- 操作的 Key
local key = KEYS[1]
local postId = ARGV[1] -- 帖子ID
local expireSeconds = ARGV[2] -- 过期时间（秒）

redis.call("BF.ADD", key, postId)
-- 设置过期时间
redis.call("EXPIRE", key, expireSeconds)
return 0
