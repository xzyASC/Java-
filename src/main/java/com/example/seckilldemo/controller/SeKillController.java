package com.example.seckilldemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.seckilldemo.config.AccessLimit;
import com.example.seckilldemo.entity.TOrder;
import com.example.seckilldemo.entity.TSeckillOrder;
import com.example.seckilldemo.entity.TUser;
import com.example.seckilldemo.exception.GlobalException;
import com.example.seckilldemo.rabbitmq.MQSender;
import com.example.seckilldemo.service.ITGoodsService;
import com.example.seckilldemo.service.ITOrderService;
import com.example.seckilldemo.service.ITSeckillOrderService;
import com.example.seckilldemo.utils.JsonUtil;
import com.example.seckilldemo.vo.GoodsVo;
import com.example.seckilldemo.vo.RespBean;
import com.example.seckilldemo.vo.RespBeanEnum;
import com.example.seckilldemo.vo.SeckillMessage;
import com.wf.captcha.ArithmeticCaptcha;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 这个就是实现秒杀功能的
 *
 * @author: LC
 * @date 2022/3/4 11:34 上午
 * @ClassName: SeKillController
 */
@Slf4j
@Controller
@RequestMapping("/seckill")
@Api(value = "秒杀", tags = "秒杀")
public class SeKillController implements InitializingBean {

    @Autowired
    private ITGoodsService itGoodsService;
    @Autowired
    private ITSeckillOrderService itSeckillOrderService;
    @Autowired
    private ITOrderService orderService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MQSender mqSender;
    @Autowired
    private RedisScript<Long> redisScript;

    private Map<Long, Boolean> EmptyStockMap = new HashMap<>();


    @ApiOperation("获取验证码")
    @GetMapping(value = "/captcha")
    public void verifyCode(TUser tUser, Long goodsId, HttpServletResponse response) {
        if (tUser == null || goodsId < 0) {
            throw new GlobalException(RespBeanEnum.REQUEST_ILLEGAL);
        }
        //设置请求头为输出图片的类型
        response.setContentType("image/jpg");
        response.setHeader("Pargam", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        //生成验证码
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32, 3);
        redisTemplate.opsForValue().set("captcha:" + tUser.getId() + ":" + goodsId, captcha.text(), 300, TimeUnit.SECONDS);
        try {
            captcha.out(response.getOutputStream());
        } catch (IOException e) {
            log.error("验证码生成失败", e.getMessage());
        }
    }

    /**
     * 获取秒杀地址
     *
     * @param tuser
     * @param goodsId
     * @param captcha
     * @return com.example.seckilldemo.vo.RespBean
     * @author LiChao
     * @operation add
     * @date 4:04 下午 2022/3/9
     **/
    @ApiOperation("获取秒杀地址")
    @AccessLimit(second = 5, maxCount = 5, needLogin = true)
    @GetMapping(value = "/path")
    @ResponseBody
    public RespBean getPath(TUser tuser, Long goodsId, String captcha, HttpServletRequest request) {
        if (tuser == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
//        ValueOperations valueOperations = redisTemplate.opsForValue();
        //限制访问次数，5秒内访问5次
//        String uri = request.getRequestURI();
//        captcha = "0";
//        Integer count = (Integer) valueOperations.get(uri + ":" + tuser.getId());
//        if (count == null) {
//            valueOperations.set(uri + ":" + tuser.getId(), 1, 5, TimeUnit.SECONDS);
//        } else if (count < 5) {
//            valueOperations.increment(uri + ":" + tuser.getId());
//        } else {
//            return RespBean.error(RespBeanEnum.ACCESS_LIMIT_REACHED);
//        }


        boolean check = orderService.checkCaptcha(tuser, goodsId, captcha);
        if (!check) {
            return RespBean.error(RespBeanEnum.ERROR_CAPTCHA);
        }
        String str = orderService.createPath(tuser, goodsId);
        return RespBean.success(str);
    }


    /**
     * 获取秒杀结果
     *
     * @param tUser
     * @param goodsId
     * @return orderId 成功 ；-1 秒杀失败 ；0 排队中
     * @author LiChao
     * @operation add
     * @date 7:04 下午 2022/3/8
     **/
    @ApiOperation("获取秒杀结果")
    @GetMapping("getResult")
    @ResponseBody
    public RespBean getResult(TUser tUser, Long goodsId) {
        if (tUser == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        Long orderId = itSeckillOrderService.getResult(tUser, goodsId);
        return RespBean.success(orderId);
    }

    /**
     * 点击秒杀按钮就会执行该方法，会自动对选定的商品进行数据库扣减，达到商品秒杀
     *
     * @param user
     * @param goodsId
     * @return java.lang.String
     * @author LC
     * @operation add
     * @date 11:36 上午 2022/3/4
     **/
    @ApiOperation("秒杀功能")
    @RequestMapping(value = "/{path}/doSeckill", method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSecKill(@PathVariable String path, TUser user, Long goodsId) {
        //若用户还未登录,则不能进行秒杀
        if (user == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        //优化后代码,得到redis的String类型数据结构操作对象
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //检查用户的请求路径,若不满足,则说明路径不合法
        boolean check = orderService.checkPath(user, goodsId, path);
        if (!check) {
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
        }

        //判断是否重复抢购(在后面代码中已经将抢购过了的用户订单进行了记录,存入redis中),检查redis中是否有订单即可判断
        TSeckillOrder tSeckillOrder = (TSeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if (tSeckillOrder != null) {
            return RespBean.error(RespBeanEnum.REPEATE_ERROR);
        }
        /*内存标记,减少Redis的访问,若检查到已经为true,说明库存已经为0了,没必要再去查redis,为什么
          叫内存标记呢？因为这是创建的一个对象,在JVM的堆中,指针指向堆,指针在栈帧中,所以这个对象是在内存中
         */
        if (EmptyStockMap.get(goodsId)) {
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        //预减库存,先在redis中减少,不急着改数据库mysql,后面将此代码写到lua脚本中了,在redis中执行
//        Long stock = valueOperations.decrement("seckillGoods:" + goodsId);
        Long stock = (Long) redisTemplate.execute(redisScript, Collections.singletonList("seckillGoods:" + goodsId), Collections.EMPTY_LIST);
        //预减库存发现小于0,说明剩余量不足
        if (stock < 0) {
            //说明现在库存已经不足了,直接标记,避免后面库存已经不足了还要频繁的访问redis
            EmptyStockMap.put(goodsId, true);
            //再将redis中的数据恢复的原状
            valueOperations.increment("seckillGoods:" + goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        //创建消息对象,mq中发送的消息体Body就是这个对象
        SeckillMessage seckillMessag = new SeckillMessage(user, goodsId);
        mqSender.sendSeckillMessage(JsonUtil.object2JsonStr(seckillMessag));
        //返回的数为0,表示成功,展示给页面,后台的mqReceiver线程异步执行
        return RespBean.success(0);
    }

    /**
     * 秒杀功能-废弃，废弃的原因是用的thymeleaf和model做的页面，前后端不分离，现在将其优化为Vue格式的
     * 前后端分离格式，将数据存放于RespBean对象中进行页面展示，已经被ajax和RespBean替代
     * @param model
     * @param user
     * @param goodsId
     * @return java.lang.String
     * @author LC
     * @operation add
     * @date 11:36 上午 2022/3/4
     **/
    @ApiOperation("秒杀功能-废弃")
    @RequestMapping(value = "/doSeckill2", method = RequestMethod.POST)
    public String doSecKill2(Model model, TUser user, Long goodsId) {
        model.addAttribute("user", user);
        GoodsVo goodsVo = itGoodsService.findGoodsVobyGoodsId(goodsId);
        if (goodsVo.getStockCount() < 1) {
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "secKillFail";
        }
        //判断是否重复抢购
        TSeckillOrder seckillOrder = itSeckillOrderService.getOne(new QueryWrapper<TSeckillOrder>().eq("user_id", user.getId()).eq("goods_id", goodsId));
        if (seckillOrder != null) {
            model.addAttribute("errmsg", RespBeanEnum.REPEATE_ERROR.getMessage());
            return "secKillFail";
        }
        TOrder tOrder = orderService.secKill(user, goodsVo);
        model.addAttribute("order", tOrder);
        model.addAttribute("goods", goodsVo);
        return "orderDetail";
    }

    /**
     * 系统初始化，把商品库存数量加载到Redis，只要项目一启动（初始化），就要加载该方法，将库存加载到redis中
     *
     * @param
     * @return void
     * @author LiChao
     * @operation add
     * @date 6:29 下午 2022/3/8
     **/
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> list = itGoodsService.findGoodsVo();
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        list.forEach(goodsVo -> {
            redisTemplate.opsForValue().set("seckillGoods:" + goodsVo.getId(), goodsVo.getStockCount());
            EmptyStockMap.put(goodsVo.getId(), false);
        });
    }
}
