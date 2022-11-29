package com.example.starqiangloudemo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.starqiangloudemo.dto.Result;
import com.example.starqiangloudemo.entity.Comment;

import com.example.starqiangloudemo.mapper.CommentsMapper;
import com.example.starqiangloudemo.service.ICommentService;
import com.example.starqiangloudemo.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comment> implements ICommentService {
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("qianglou.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Resource
    StringRedisTemplate stringRedisTemplate;
    private ICommentService commentProxy;

    @Override
    public Result qianglou(Comment comment) {
        //获取用户
        Long userId = UserHolder.getUser().getId();

        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                comment.getContent(), userId.toString()
        );
        int r = result.intValue();
        if (r != 0) {
            //2.1.不为0,代表没有购买资格
            return Result.fail(r == 1 ? "不能重复评论" : "其他原因");
        }

        commentProxy = (ICommentService) AopContext.currentProxy();


        return Result.ok();
    }

    @Transactional
    public void CommentInSql(Comment comment) {
        //写入数据库
        save(comment);


    }

    @Resource
    private RedissonClient redissonClient;


    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    //当前类初始化完毕时执行
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new CommentsServiceImpl.CommentHandler());
    }

    private class CommentHandler implements Runnable {
        String queueName = "stream.orders";

        @Override
        public void run() {
            while (true) {
                try {
                    //1.获取redis消息队列中的订单信息 XREADGROUP GROUP g1 c1 count 1 BLOCK 2000 STREAMS streams.order >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    //2.判断消息获取是否成功
                    if (list == null || list.isEmpty()) {
                        //2.1.如果获取失败,说明没有消息,继续下一次循环
                        continue;
                    }
                    //3.解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    Comment comment = BeanUtil.fillBeanWithMap(values, new Comment(), true);
                    //4.如果获取成功,可以下单
                    handleComment(comment);

                } catch (Exception e) {
                    log.error("处理订单异常", e);

                }
            }
        }
    }

    private void handleComment(Comment comment) {
        //获取用户
        Long userId = comment.getUserId();
        RLock lock = redissonClient.getLock("lcok:order:" + userId);
        boolean isLock = lock.tryLock();
        //判断是否获取成功(双重保险)
        if (!isLock) {
            //获取锁失败,返回错误信息,或者重试
            log.error("不允许重复下单");
            return;
        }

        try {
            //获取代理对象（事务）
            commentProxy.CommentInSql(comment);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } finally {
            //释放锁
            lock.unlock();
        }
    }


}
