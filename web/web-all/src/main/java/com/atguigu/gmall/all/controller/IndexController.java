package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author ca ca
 * @Date 2020/5/1
 */
@Controller
@RequestMapping
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @GetMapping({"/", "index.html"})
    public String index(HttpServletRequest request){
        Result list = productFeignClient.getBaseCategoryList();
        request.setAttribute("list",list.getData());
        return "index/index";
    }
}
