package com.example.seckilldemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.seckilldemo.entity.TSeckillOrder;
import com.example.seckilldemo.entity.TUser;

/**
 * 秒杀订单表 服务类
 */
public interface ITSeckillOrderService extends IService<TSeckillOrder> {


    /**
     * 获取秒杀结果
     **/
    Long getResult(TUser tUser, Long goodsId);
}
