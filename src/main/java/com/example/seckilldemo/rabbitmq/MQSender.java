package com.example.seckilldemo.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息发送者
 */
@Service
@Slf4j
public class MQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;


    /**
     * 发送秒杀信息，其它方法调用该方法，就间接调用了rabbitTemplate向指定队列中发送消息
     * 要想调用方法向消息队列中发送消息，直接调用该方法即可
     * 主线程只管往消息队列中发消息即可，就表示主线程以执行完成，至于怎么执行任务，那就是后台线程该管的事了
     **/
    public void sendSeckillMessage(String message) {
        log.info("发送消息" + message);
        rabbitTemplate.convertAndSend("seckillExchange", "seckill.message", message);
    }

}
