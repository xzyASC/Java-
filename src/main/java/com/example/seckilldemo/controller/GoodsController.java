package com.example.seckilldemo.controller;

import com.example.seckilldemo.entity.TUser;
import com.example.seckilldemo.service.ITGoodsService;
import com.example.seckilldemo.service.ITUserService;
import com.example.seckilldemo.vo.DetailVo;
import com.example.seckilldemo.vo.GoodsVo;
import com.example.seckilldemo.vo.RespBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 这里面的API是用于查询数据库的所有商品信息和和根据Id查询某个商品的详细信息，
 * 包括商品信息和秒杀倒计时（前端用户展示动态变化）
 * @author: LC
 * @date 2022/3/2 5:56 下午
 * @ClassName: GoodsController
 */
@Controller
@RequestMapping("goods")
@Api(value = "商品", tags = "商品")
public class GoodsController {

    @Autowired
    private ITUserService itUserService;
    @Autowired
    private ITGoodsService itGoodsService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;


    @ApiOperation("商品详情")
    @RequestMapping(value = "/goodsDetail2/{goodsId}", produces = "text/html;charset=utf-8", method = RequestMethod.GET)
    @ResponseBody
    public String toDetail2(Model model, TUser user, @PathVariable Long goodsId, HttpServletRequest request, HttpServletResponse response) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsDetail:" + goodsId);
        if (!StringUtils.isEmpty(html)) {
            return html;
        }

        model.addAttribute("user", user);
        GoodsVo goodsVo = itGoodsService.findGoodsVobyGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();
        //秒杀状态
        int seckillStatus = 0;
        //秒杀倒计时
        int remainSeconds = 0;

        if (nowDate.before(startDate)) {
            //秒杀还未开始0
            remainSeconds = (int) ((startDate.getTime() - nowDate.getTime()) / 1000);
        } else if (nowDate.after(endDate)) {
            //秒杀已经结束
            seckillStatus = 2;
            remainSeconds = -1;
        } else {
            //秒杀进行中
            seckillStatus = 1;
            remainSeconds = 0;
        }
        model.addAttribute("remainSeconds", remainSeconds);
        model.addAttribute("goods", goodsVo);
        model.addAttribute("seckillStatus", seckillStatus);

        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsDetail", webContext);
        if (!StringUtils.isEmpty(html)) {
            valueOperations.set("goodsDetail:" + goodsId, html, 60, TimeUnit.SECONDS);
        }

        return html;
    }

    /**
     * 用户登录成功后自动跳转到商品列表，也就是自动调用该方法，需要两张表联合查询，因为要展示的数据在两张表中，组成一个
     * 新的实体类，结合两张表的查询结果重新组装成GoodsVo，该属性就是商品列表的详情，至于model参数，就是在Java代码中
     * 进行页面跳转的必要条件
     * @param model
     * @param user
     * @param request
     * @param response
     * @return
     */
    @ApiOperation("商品列表")
    @RequestMapping(value = "/toList", produces = "text/html;charset=utf-8", method = RequestMethod.GET)
    @ResponseBody
    public String toList(Model model, TUser user, HttpServletRequest request, HttpServletResponse response) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsList");
        if (!StringUtils.isEmpty(html)) {
            return html;
        }

        model.addAttribute("user", user);
        model.addAttribute("goodsList", itGoodsService.findGoodsVo());

        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsList", webContext);
        if (!StringUtils.isEmpty(html)) {
            valueOperations.set("goodsList", html, 60, TimeUnit.SECONDS);
        }
        //与JSP一样,不是前后端分离的,在Java代码中进行页面跳转,将数据存在model对象中
        return html;
    }

    /**
     * 进入商品详情的大厅中，根据一个商品的id查询其详情，会根据id查询该商品的具体信息，只要点击了某一个商品
     * 的详情，就要自动向该方法发起请求，获取商品信息和秒杀剩余时间
     * @param user
     * @param goodsId
     * @return
     */
    @ApiOperation("商品详情")
    @RequestMapping(value = "/detail/{goodsId}", method = RequestMethod.GET)
    @ResponseBody
    public RespBean toDetail(TUser user, @PathVariable Long goodsId) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        GoodsVo goodsVo = itGoodsService.findGoodsVobyGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();
        //秒杀状态,初始化为0,表示现在是否处于秒杀阶段
        int seckillStatus = 0;
        //秒杀倒计时,初始化为0,用来表示按钮颜色程度,0表示可以点击秒杀按钮,否则就不能点击
        int remainSeconds = 0;

        if (nowDate.before(startDate)) {
            //秒杀还未开始0,将剩余时间计算出来,交给前端展示并进行倒计时处理
            remainSeconds = (int) ((startDate.getTime() - nowDate.getTime()) / 1000);
        } else if (nowDate.after(endDate)) {
            //秒杀已经结束
            seckillStatus = 2;
            remainSeconds = -1;
        } else {
            //秒杀进行中
            seckillStatus = 1;
            remainSeconds = 0;
        }
//        model.addAttribute("remainSeconds", remainSeconds);
//        model.addAttribute("goods", goodsVo);
//        model.addAttribute("seckillStatus", seckillStatus);

//        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
//        html = thymeleafViewResolver.getTemplateEngine().process("goodsDetail", webContext);
//        if (!StringUtils.isEmpty(html)) {
//            valueOperations.set("goodsDetail:" + goodsId, html, 60, TimeUnit.SECONDS);
//        }
        DetailVo detailVo = new DetailVo();
        detailVo.setTUser(user);
        detailVo.setGoodsVo(goodsVo);
        detailVo.setRemainSeconds(remainSeconds);
        detailVo.setSecKillStatus(seckillStatus);
        return RespBean.success(detailVo);
    }
    /**
     * 对于上面为什么要进行优化（注释的部分），之前是用thymeleaf结合model进行页面跳转和页面渲染，
     * 与JSP和session,el表达式原理相同，将数据存到session里面利用Java代码进行页面跳转，用el表达式
     * 进行展示，thymeleaf就相当于JSP，model就相当于session存储数据，所以thymeleaf也是被淘汰的，未
     * 实现前后端分离，model对象里面就存储着页面要展示的数据
     * 优化后就是利用ajax请求，类似于Vue，之前是将数据存到model里面进行页面展示，现在是封装成一个对象，
     * 将要展示的数据封装成一个对象传给前端，利用对象来展示数据
     */
}
