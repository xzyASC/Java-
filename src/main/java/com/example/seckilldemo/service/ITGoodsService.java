package com.example.seckilldemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.seckilldemo.entity.TGoods;
import com.example.seckilldemo.vo.GoodsVo;

import java.util.List;

/**
 * 商品表 服务类
 */
public interface ITGoodsService extends IService<TGoods> {


    /**
     * 返回商品列表
     **/
    List<GoodsVo> findGoodsVo();

    /**
     * 获取商品详情
     **/
    GoodsVo findGoodsVobyGoodsId(Long goodsId);
}
