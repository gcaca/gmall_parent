package com.atguigu.gmall.list.repository;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

/**
 * @Author ca ca
 * @Date 2020/5/2
 */
@Component
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {

}
