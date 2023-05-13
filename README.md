

1. 项目框架搭建
   1. SpringBoot环境搭建
   2. 集成Thymeleaf,RespBean
   3. MyBatis
2. 分布式会话
   1. 用户登录
      1. 设计数据库
      2. 明文密码二次MD5加密
      3. 参数校验+全局异常处理
   2. 共享Session
      1. SpringSession
      2. Redis
3. 功能开发
   1. 商品列表
   2. 商品详情
   3. 抢购
   4. 订单详情
4. 系统压测
   1. JMeter
   2. 自定义变量模拟多用户
   3. JMeter命令行的使用
   4. 正式压测
      1. 商品列表
      2. 秒杀
5. 接口优化
   1. Redis预减库存减少数据库的访问
   2. 内存标记减少Redis的访问
   3. RabbitMQ异步下单
      1. SpringBoot整合RabbitMQ
      2. 交换机


## 软件架构

|                         技术                          | 版本  |                            说明                            |
| :---------------------------------------------------: | :---: | :--------------------------------------------------------: |
|                      Spring Boot                      | 2.6.4 |                                                            |
|                         MySQL                         |   8   |                                                            |
| [MyBatis Plus](https://github.com/baomidou/generator) | 3.5.1 |                                                            |
|                       Swagger2                        | 2.9.2 |        Swagger-models2.9.2版本报错，使用的是1.5.22         |
|         [Kinfe4j](https://doc.xiaominfo.com)          | 2.0.9 | 感觉比Swagger UI漂亮的一个工具，访问地址是ip:端口/doc.html |
|                   Spring Boot Redis                   |       |                                                            |



## 使用说明

登录页面：http://localhost:8080/login/toLogin

接口文档页面：http://localhost:8080/doc.html#/home

代码生成器：https://gitee.com/guizhizhe/code-generator.git

sqldoc：创建表语句和回滚压测数据





## 注意事项

1. 代码运行时一定要用localhost，原因可以看下CookieUtil类
2. 代码生成时没有加去掉表头，导致代码和视频中不一样，命名也有的不一样，

