package com.example.seckilldemo.exception;

import com.example.seckilldemo.vo.RespBean;
import com.example.seckilldemo.vo.RespBeanEnum;

/**
 * 这是自定义的全局异常
 * @author: LC
 * @date 2022/3/2 5:32 下午
 * @ClassName: GlobalException
 */
public class GlobalException extends RuntimeException {

    private RespBeanEnum respBeanEnum;

    /**
     * 无参构造，当类进行初始化时执行init方法就要执行该无参构造
     * @return
     */
    public RespBeanEnum getRespBeanEnum() {
        return respBeanEnum;
    }

    public void setRespBeanEnum(RespBeanEnum respBeanEnum) {
        this.respBeanEnum = respBeanEnum;
    }

    /**
     * 有参构造，枚举类中的元素都是一个对象，底层语法糖对枚举类中的属性通过构造器进行创建对象
     * @param respBeanEnum
     */
    public GlobalException(RespBeanEnum respBeanEnum) {
        this.respBeanEnum = respBeanEnum;
    }
}
