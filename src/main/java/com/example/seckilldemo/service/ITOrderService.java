package com.example.seckilldemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.seckilldemo.entity.TOrder;
import com.example.seckilldemo.entity.TUser;
import com.example.seckilldemo.vo.GoodsVo;
import com.example.seckilldemo.vo.OrderDeatilVo;
import org.apache.catalina.User;

/**
 * 服务类
 */
public interface ITOrderService extends IService<TOrder> {

    /**
     * 秒杀
     **/
    TOrder secKill(TUser user, GoodsVo goodsVo);


    /**
     * 订单详情方法
     **/
    OrderDeatilVo detail(Long orderId);

    /**
     * 获取秒杀地址
     **/
    String createPath(TUser user, Long goodsId);

    /**
     * 校验秒杀地址
     **/
    boolean checkPath(TUser user, Long goodsId, String path);

    /**
     * 校验验证码
     **/
    boolean checkCaptcha(TUser tuser, Long goodsId, String captcha);
}
