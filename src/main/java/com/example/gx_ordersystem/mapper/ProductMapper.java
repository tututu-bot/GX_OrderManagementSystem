package com.example.gx_ordersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gx_ordersystem.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
