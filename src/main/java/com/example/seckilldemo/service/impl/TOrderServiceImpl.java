package com.example.seckilldemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.seckilldemo.entity.TOrder;
import com.example.seckilldemo.entity.TSeckillGoods;
import com.example.seckilldemo.entity.TSeckillOrder;
import com.example.seckilldemo.entity.TUser;
import com.example.seckilldemo.exception.GlobalException;
import com.example.seckilldemo.mapper.TOrderMapper;
import com.example.seckilldemo.mapper.TSeckillOrderMapper;
import com.example.seckilldemo.service.ITGoodsService;
import com.example.seckilldemo.service.ITOrderService;
import com.example.seckilldemo.service.ITSeckillGoodsService;
import com.example.seckilldemo.service.ITSeckillOrderService;
import com.example.seckilldemo.utils.MD5Util;
import com.example.seckilldemo.utils.UUIDUtil;
import com.example.seckilldemo.vo.GoodsVo;
import com.example.seckilldemo.vo.OrderDeatilVo;
import com.example.seckilldemo.vo.RespBeanEnum;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;


import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 服务实现类
 */
@Service
@Primary
public class TOrderServiceImpl extends ServiceImpl<TOrderMapper, TOrder> implements ITOrderService {

    @Autowired
    private ITSeckillGoodsService itSeckillGoodsService;
    @Autowired
    private TOrderMapper tOrderMapper;
    @Autowired
    private ITSeckillOrderService itSeckillOrderService;
    @Autowired
    private ITGoodsService itGoodsService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 点击秒杀，将订单信息发送给mq，由mq来调用该方法实现秒杀抢购，这个界面是订单详情界面，说明该用户已经下单成功，
     * 下单失败的就在前面就已经返回错误对象了
     * @param user    用户对象
     * @param goodsVo 商品对象
     * @return
     */
    @Transactional
    @Override
    public TOrder secKill(TUser user, GoodsVo goodsVo) {
        ValueOperations valueOperations = redisTemplate.opsForValue();

        //能到这里,说明库存已经是足够的,放心大胆的扣减库存即可
        TSeckillGoods seckillGoods = itSeckillGoodsService.getOne(new QueryWrapper<TSeckillGoods>().eq("goods_id", goodsVo.getId()));
        //按照逻辑先进行扣减，这个结果一定是>=0,这个的目的就是判断库存数量,好利用这个结果来更新redis
        seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);
//        itSeckillGoodsService.updateById(seckillGoods);
//        boolean seckillGoodsResult = itSeckillGoodsService.update(new UpdateWrapper<TSeckillGoods>()
//                .set("stock_count", seckillGoods.getStockCount())
//                .eq("id", seckillGoods.getId())
//                .gt("stock_count", 0)
//        );
        //此sql语句的作用就是goods_id相等，且库存数量大于0时执行该sql语句，将库存数量减一,乐观锁进行扣除
        boolean seckillGoodsResult = itSeckillGoodsService.update(new UpdateWrapper<TSeckillGoods>()
                .setSql("stock_count = " + "stock_count-1")
                .eq("goods_id", goodsVo.getId())
                .gt("stock_count", 0)
        );
//        if (!seckillGoodsResult) {
//            return null;
//        }

        if (seckillGoods.getStockCount() < 1) {
            //利用刚才判断的结果,发现数据库的库存已经为0,此时更新redis
            valueOperations.set("isStockEmpty:" + goodsVo.getId(), "0");
            return null;
        }
        //用户抢购该商品,就要同时生成order订单表和secKillOrder秒杀订单表
        //生成订单
        TOrder order = new TOrder();
        order.setUserId(user.getId());
        order.setGoodsId(goodsVo.getId());
        order.setDeliveryAddrId(0L);
        order.setGoodsName(goodsVo.getGoodsName());
        order.setGoodsCount(1);
        order.setGoodsPrice(seckillGoods.getSeckillPrice());
        order.setOrderChannel(1);
        order.setStatus(0);
        order.setCreateDate(new Date());
        tOrderMapper.insert(order);
        //生成秒杀订单
        TSeckillOrder tSeckillOrder = new TSeckillOrder();
        tSeckillOrder.setUserId(user.getId());
        tSeckillOrder.setOrderId(order.getId());
        tSeckillOrder.setGoodsId(goodsVo.getId());
        itSeckillOrderService.save(tSeckillOrder);
        /*这样做的目的就是保存已经抢购了的用户信息,当下次该用户再来抢时就说明该用户已经抢购过,下次判断就直接在lua
          脚本中进行判断*/
        redisTemplate.opsForValue().set("order:" + user.getId() + ":" + goodsVo.getId(), tSeckillOrder);
        return order;
    }

    /**
     * 订单详情，根据传来的订单Id查询对应的订单，并查询该订单对应的商品信息
     * @param orderId
     * @return
     */
    @Override
    public OrderDeatilVo detail(Long orderId) {
        if (orderId == null) {
            throw new GlobalException(RespBeanEnum.ORDER_NOT_EXIST);
        }
        TOrder tOrder = tOrderMapper.selectById(orderId);
        GoodsVo goodsVobyGoodsId = itGoodsService.findGoodsVobyGoodsId(tOrder.getGoodsId());
        OrderDeatilVo orderDeatilVo = new OrderDeatilVo();
        orderDeatilVo.setTOrder(tOrder);
        orderDeatilVo.setGoodsVo(goodsVobyGoodsId);
        return orderDeatilVo;
    }

    @Override
    public String createPath(TUser user, Long goodsId) {
        String str = MD5Util.md5(UUIDUtil.uuid() + "123456");
        redisTemplate.opsForValue().set("seckillPath:" + user.getId() + ":" + goodsId, str, 1, TimeUnit.MINUTES);
        return str;
    }

    @Override
    public boolean checkPath(TUser user, Long goodsId, String path) {
        if (user == null || goodsId < 0 || StringUtils.isEmpty(path)) {
            return false;
        }
        String redisPath = (String) redisTemplate.opsForValue().get("seckillPath:" + user.getId() + ":" + goodsId);
        return path.equals(redisPath);
    }

    @Override
    public boolean checkCaptcha(TUser user, Long goodsId, String captcha) {
        if (user == null || goodsId < 0 || StringUtils.isEmpty(captcha)) {
            return false;
        }
        String redisCaptcha = (String) redisTemplate.opsForValue().get("captcha:" + user.getId() + ":" + goodsId);
        return captcha.equals(redisCaptcha);
    }
}
