package com.example.seckilldemo.rabbitmq;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.seckilldemo.entity.TSeckillOrder;
import com.example.seckilldemo.entity.TUser;
import com.example.seckilldemo.service.ITGoodsService;
import com.example.seckilldemo.service.ITOrderService;
import com.example.seckilldemo.service.ITSeckillOrderService;
import com.example.seckilldemo.utils.JsonUtil;
import com.example.seckilldemo.vo.GoodsVo;
import com.example.seckilldemo.vo.RespBean;
import com.example.seckilldemo.vo.RespBeanEnum;
import com.example.seckilldemo.vo.SeckillMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 消息消费者，这个就主要实现异步操作的，这个线程是一直启动的，用户主线程只要将消息发送给mq即可，就可以执行下面的逻辑了，
 * 后台的异步线程就会执行MQReceiver来执行下单操作
 * @author: LC
 * @date 2022/3/7 7:44 下午
 * @ClassName: MQReceiver
 */
@Service
@Slf4j
public class MQReceiver {

    @Autowired
    private ITGoodsService itGoodsServicel;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ITOrderService itOrderService;


    /**
     * 下单操作，这里直接绑定队列，交换机和队列已经RabbitmqTopicConfig中创建并绑定
     * 只要消息队列中一有消息，后台线程就会立即执行该消息队列中的任务
     * @param
     * @return void
     * @author LiChao
     * @operation add
     * @date 6:48 下午 2022/3/8
     **/
    @RabbitListener(queues = "seckillQueue")
    public void receive(String message) {
        log.info("接收消息：" + message);
        //将Json字符串转换成对象
        SeckillMessage seckillMessage = JsonUtil.jsonStr2Object(message, SeckillMessage.class);
        Long goodsId = seckillMessage.getGoodsId();
        TUser user = seckillMessage.getTUser();
        //向数据库中查询该物品的详细信息
        GoodsVo goodsVo = itGoodsServicel.findGoodsVobyGoodsId(goodsId);
        //如果库存不足
        if (goodsVo.getStockCount() < 1) {
            return;
        }
        //判断是否重复抢购,在redis中根据用户Id查询对应的订单,此时未考虑同一用户同一时刻来抢购
        TSeckillOrder tSeckillOrder = (TSeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if (tSeckillOrder != null) {
            return;
        }
        //下单操作
        itOrderService.secKill(user, goodsVo);

    }

}
