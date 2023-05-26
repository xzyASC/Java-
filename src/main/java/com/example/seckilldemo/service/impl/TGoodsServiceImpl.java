package com.example.seckilldemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.seckilldemo.entity.TGoods;
import com.example.seckilldemo.mapper.TGoodsMapper;
import com.example.seckilldemo.service.ITGoodsService;
import com.example.seckilldemo.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品表 服务实现类
 */
@Service
@Primary
public class TGoodsServiceImpl extends ServiceImpl<TGoodsMapper, TGoods> implements ITGoodsService {


    @Autowired
    private TGoodsMapper tGoodsMapper;

    /**
     * 查询全部商品信息，两张表的联合查询，查询结果是商品的展示信息，findGoodsVo是自定义的写法
     * @return
     */
    @Override
    public List<GoodsVo> findGoodsVo() {
        return tGoodsMapper.findGoodsVo();
    }

    /**
     * 根据全部的商品信息中某个商品的id查询特定的商品信息，点击详情，根据商品Id查询数据，这个方法也是自定义的
     * @param goodsId
     * @return
     */
    @Override
    public GoodsVo findGoodsVobyGoodsId(Long goodsId) {
        return tGoodsMapper.findGoodsVobyGoodsId(goodsId);
    }
}
