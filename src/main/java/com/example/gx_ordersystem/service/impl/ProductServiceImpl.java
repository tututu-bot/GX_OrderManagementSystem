package com.example.gx_ordersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.gx_ordersystem.entity.Product;
import com.example.gx_ordersystem.mapper.ProductMapper;
import com.example.gx_ordersystem.service.ProductService;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
}
