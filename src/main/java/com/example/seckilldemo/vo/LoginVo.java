package com.example.seckilldemo.vo;


import com.example.seckilldemo.validator.IsMobile;

import javax.validation.constraints.NotNull;

/**
 * 登录传参
 *
 * @author: LC
 * @date 2022/3/2 2:01 下午
 * @ClassName: LoginVo
 */
public class LoginVo {

    /**
     * @NotNull 这是validation组件的配置，提供了一系列API对属性进行限制，可用作对用户输入的数据进行验证
     */

    @NotNull
//    @IsMobile
    private String mobile;

    @NotNull
//    @Length(min = 32)
    private String password;

    @Override
    public String toString() {
        return "LoginVo{" +
                "mobile='" + mobile + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
