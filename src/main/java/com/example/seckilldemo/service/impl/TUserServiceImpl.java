package com.example.seckilldemo.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.seckilldemo.entity.TUser;
import com.example.seckilldemo.exception.GlobalException;
import com.example.seckilldemo.mapper.TUserMapper;
import com.example.seckilldemo.service.ITUserService;
import com.example.seckilldemo.utils.CookieUtil;
import com.example.seckilldemo.utils.MD5Util;
import com.example.seckilldemo.utils.UUIDUtil;
import com.example.seckilldemo.vo.LoginVo;
import com.example.seckilldemo.vo.RespBean;
import com.example.seckilldemo.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author LiChao
 * @since 2022-03-02
 */
@Service
@Primary
public class TUserServiceImpl extends ServiceImpl<TUserMapper, TUser> implements ITUserService {


    @Autowired
    private TUserMapper tUserMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public RespBean doLongin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response) {
        String mobile = loginVo.getMobile();
        String password = loginVo.getPassword();
        //参数校验,若其中一个为空,就直接报错
        if (StringUtils.isEmpty(mobile) || StringUtils.isEmpty(password)) {
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }
        //TODO 因为我懒测试的时候，把手机号码和密码长度校验去掉了，可以打开。页面和实体类我也注释了，记得打开
//        if (!ValidatorUtil.isMobile(mobile)) {
//            return RespBean.error(RespBeanEnum.MOBILE_ERROR);
//        }
        TUser user = tUserMapper.selectById(mobile);
        System.out.println(user);
        if (user == null) {
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }
        //判断密码是否正确
        /**
            该bug已经修改，就是处理加密时出现了错误，没加 ""，检验输入的密码的MD5二次加密后与数据库中的数据是否相等
         */
        if (!MD5Util.formPassToDBPass(password, user.getSalt()).equals(user.getPassword())) {
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }
//        生成Cookie,这个Cookie是来验证用户是否登录,若用户登陆过,就一定有其对应的Cookie,不是起的拦截器的作用
        String userTicket = UUIDUtil.uuid();

        //将用户信息存入redis,用用户登录的Cookie作为key,用户user作为value,一个用户对应一个Cookie
        redisTemplate.opsForValue().set("user:" + userTicket, user);

        request.getSession().setAttribute(userTicket, user);
        CookieUtil.setCookie(request, response, "userTicket", userTicket);
        return RespBean.success(user);
    }

    /**
     * 通过前端传来的Cookie（userTicket）在redis中获取对应的用户信息，因为前面的登录功能
     * 在检验用户存在时已经将Cookie和user作为key-value存储在redis中了，以后想用user对象
     * 就调用这个方法传入userTicket参数即可，浏览器每次请求都要获取用户信息，正是因为通过前端的
     * Cookie来获取用户信息的，好知道这么多请求都是同一个用户发送过来的
     * @param userTicket
     * @param request
     * @param response
     * @return
     */
    @Override
    public TUser getUserByCookie(String userTicket, HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isEmpty(userTicket)) {
            return null;
        }
        TUser user = (TUser) redisTemplate.opsForValue().get("user:" + userTicket);
        if (user != null) {
            CookieUtil.setCookie(request, response, "userTicket", userTicket);
        }
        return user;
    }

    /**
     * 密码更新，虽然没有调用这个接口，但还是要实现一下，目的就在于说明更新数据库时一定要把redis缓存删除
     * 目的就是保证数据的一致性
     * @param userTicket
     * @param password
     * @param request
     * @param response
     * @return
     */
    @Override
    public RespBean updatePassword(String userTicket, String password, HttpServletRequest request, HttpServletResponse response) {
        TUser user = getUserByCookie(userTicket, request, response);
        if (user == null) {
            throw new GlobalException(RespBeanEnum.MOBILE_NOT_EXIST);
        }
        user.setPassword(MD5Util.inputPassToDBPass(password, user.getSalt()));
        int result = tUserMapper.updateById(user);
        if (1 == result) {
            //删除Redis
            redisTemplate.delete("user:" + userTicket);
            return RespBean.success();
        }
        return RespBean.error(RespBeanEnum.PASSWORD_UPDATE_FAIL);
    }


}
