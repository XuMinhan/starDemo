--1.参数列表
--1.1评论内容
local commentContent = ARGV[1]
--1.2用户id
local userId = ARGV[2]

--2.数据key

local commentKey = 'qianglou:'


--3.脚本业务

--3.1.判断用户是否已评论 SISMEMBER orderKey userId
if (redis.call('sismember', commentKey, userId) == 1) then
    --3.3.存在,说明是重复评论,返回1
    return 1
end

--3.5.(保存用户)sadd orderKey userId
redis.call('sadd', commentKey, userId)
--3.6.发送消息到队列当中, XADD stream.orders * k1 v1 k2 v2 ...
redis.call('xadd','stream.orders','*','userId',userId,'content',commentContent)
return 0
