package com.example.seckilldemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.seckilldemo.entity.TUser;
import com.example.seckilldemo.vo.LoginVo;
import com.example.seckilldemo.vo.RespBean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * 用户表 服务类
 * </p>
 */

public interface ITUserService extends IService<TUser> {


    /**
     * 登录方法
     **/
    RespBean doLongin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response);

    /**
     * 根据cookie获取用户
     **/
    TUser getUserByCookie(String userTicket, HttpServletRequest request, HttpServletResponse response);


    /**
     * 更新密码
     **/
    RespBean updatePassword(String userTicket, String password, HttpServletRequest request, HttpServletResponse response);
}
