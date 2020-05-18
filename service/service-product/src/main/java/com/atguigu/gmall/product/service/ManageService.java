package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author ca ca
 * @version 1.0
 * @see
 */
public interface ManageService {
    /**
     * 查询所有一级分类
     * @return
     */
    List<BaseCategory1> getCategory1();

    /**
     * 根据一级分类id查询二级分类
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 根据二级分类id查询三级分类
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     * 根据分类id查询平台属性数据,平台属性可以挂在一级分类、二级分类和三级分类
     * 接口说明：
     * @param category1Id 查询一级分类下面的平台属性，传：category1Id，0，0；取出该分类的平台属性
     * @param category2Id 查询二级分类下面的平台属性，传：category1Id，category2Id，0;取出对应一级分类下面的平台属性与二级分类对应的平台属性
     * @param category3Id 查询三级分类下面的平台属性，传：category1Id，category2Id，category3Id；
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id);

    /**
     * 添加或修改平台属性
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据平台属性id查询平台属性及属性值封装后返回
     * @param attrId
     * @return
     */
    BaseAttrInfo getAttrValueList(Long attrId);

    /**
     * 根据spuInfo属性category3Id及分页信息查询商品表
     * @param param
     * @param spuInfo
     * @return
     */
    IPage<SpuInfo> selectPage(Page<SpuInfo> param, SpuInfo spuInfo);

    /**
     * 查询所有基本销售属性表
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * SPU 保存至数据库
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuId查询Spu Image表
     * @return
     */
    List<SpuImage> spuImageList(Long spuId);

    /**
     * 根据spuId查询 spuSaleAttr biao
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> spuSaleAttrList(Long spuId);

    /**
     * SKU 保存
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 分页查询skuInfo
     * @param skuInfoPage
     * @return
     */
    IPage<SkuInfo> selectPage(Page<SkuInfo> skuInfoPage);

    /**
     * 根据skuId 上架
     * @param skuId
     */
    void onSale(Long skuId);

    /**
     * 根据skuId 下架
     * @param skuId
     */
    void cancelSale(Long skuId);

    /**
     * 根据 skuId 查询 skuInfo
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(Long skuId);

    /**
     * 通过category3Id 获取View视图1,2,3级分类
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryViewByCategory3Id(Long category3Id);

    /**
     * 通过 skuId 只查询商品价格
     * @param skuId
     * @return
     */
    BigDecimal getSkuPrice(Long skuId);

    /**
     * 根据spuId，skuId 查询销售属性集合锁定用户购买商品
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    /**
     * 根据spuId 查询sku组合方案集合
     * @param spuId
     * @return
     */
    Map getSkuValueIdsMap(Long spuId);

    /**
     * 获取全部分类信息
     * @return
     */
    List<JSONObject> getBaseCategoryList();

    /**
     * 根据品牌id查询品牌数据
     * @param tmId
     * @return
     */
    BaseTrademark getTrademarkByTmId(Long tmId);

    /**
     * 根据skuId 查询平台属性及属性值
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrList(Long skuId);
}
