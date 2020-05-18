package com.atguigu.gmall.payment.mapper;

import com.atguigu.gmall.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author ca ca
 * @Date 2020/5/13
 */
@Mapper
public interface PaymentMapper extends BaseMapper<PaymentInfo> {
}
