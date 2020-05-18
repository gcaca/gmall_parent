package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author ca ca
 * @version 1.0
 * @see
 */
public interface BaseTrademarkService extends IService<BaseTrademark> {


    IPage<BaseTrademark> selectPage(IPage<BaseTrademark> param);
    //获取所有品牌表
    List<BaseTrademark> getTrademarkList();
}
