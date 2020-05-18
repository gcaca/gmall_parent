package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author ca ca
 * @Date 2020/5/8
 */
@Controller
public class ListController {
    @Autowired
    private ListFeignClient listFeignClient;

    @GetMapping("list.html")
    public String list(SearchParam searchParam, Model model) {
        Result<Map> result = listFeignClient.search(searchParam);
        model.addAllAttributes(result.getData());
        // 记录拼接url；
        String urlParam = makeUrlParam(searchParam);
        //处理品牌条件回显
        String trademark = makeTrademark(searchParam.getTrademark());
        //处理平台属性条件回显
        List<Map<String, String>> props = makeProps(searchParam.getProps());
        //处理排序
        Map<String, Object> orderMap = order(searchParam.getOrder());
        model.addAttribute("searchParam", searchParam);
        model.addAttribute("urlParam", urlParam);
        model.addAttribute("trademarkParam", trademark);
        if (props != null) {
            model.addAttribute("propsParamList", props);
        }
        model.addAttribute("orderMap", orderMap);
        return "list/index";
    }

    private Map<String, Object> order(String order) {
        HashMap<String, Object> orderMap = new HashMap<>();
        if (!StringUtils.isEmpty(order)) {
            String[] split = order.split(":");
            if (split != null && split.length == 2) {
                orderMap.put("type", split[0]);
                orderMap.put("sort", split[1]);
            }
        } else {
            orderMap.put("type", "1");
            orderMap.put("sort", "asc");
        }
        return orderMap;
    }

    private List<Map<String, String>> makeProps(String[] props) {
        ArrayList<Map<String, String>> lists = new ArrayList<>();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                String[] split = prop.split(":");
                if (split != null && split.length == 3) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("attrId", split[0]);
                    map.put("attrValue", split[1]);
                    map.put("attrName", split[2]);
                    lists.add(map);
                }
            }
        }
        return lists;
    }

    private String makeTrademark(String trademark) {
        if (StringUtils.isNotEmpty(trademark)) {
            String[] split = trademark.split(":");
            if (split == null && split.length == 2) {
                return "品牌" + split[1];
            }
        }
        return "";
    }

    private String makeUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();
        //判断关键字
        if (StringUtils.isNotEmpty(searchParam.getKeyword())) {
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        //判断一级分类
        if (searchParam.getCategory1Id() != null) {
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }
        //判断二级分类
        if (searchParam.getCategory2Id() != null) {
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }
        //判断三级分类
        if (searchParam.getCategory3Id() != null) {
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }
        //处理品牌
        if (searchParam.getTrademark() != null) {
            if (urlParam.length() > 0) {
                urlParam.append("&trademark=").append(searchParam.getTrademark());
            }
        }
        //判断平台属性值
        if (searchParam.getProps() != null) {
            for (String prop : searchParam.getProps()) {
                if (urlParam.length() > 0) {
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        return "list.html?" + urlParam.toString();
    }
}
