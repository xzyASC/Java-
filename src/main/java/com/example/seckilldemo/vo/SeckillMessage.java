package com.example.seckilldemo.vo;

import com.example.seckilldemo.entity.TUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 秒杀信息，用于在mq中传送订单消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillMessage {

    private TUser tUser;

    private Long goodsId;
}
