package com.example.seckilldemo.controller;


import io.swagger.annotations.Api;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试类
 *
 */
@Controller
@RequestMapping("demo")
@Api(value = "demo", tags = "demo测试类")
public class DemoController {


    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public String hello(Model model) {
        model.addAttribute("name", "example");
        return "hello";
    }
}
