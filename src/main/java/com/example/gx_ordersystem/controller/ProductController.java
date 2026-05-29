package com.example.gx_ordersystem.controller;

import com.example.gx_ordersystem.common.Result;
import com.example.gx_ordersystem.entity.Product;
import com.example.gx_ordersystem.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品管理控制器
 * 提供商品列表查询接口，所有登录用户均可访问
 */
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 查询全部商品列表
     * 请求方式: GET
     * 请求路径: /api/product
     */
    @GetMapping
    public Result<List<Product>> list() {
        return Result.success(productService.lambdaQuery()
                .orderByAsc(Product::getId)
                .list());
    }

    /**
     * 根据编码查询商品
     * 请求方式: GET
     * 请求路径: /api/product/{code}
     */
    @GetMapping("/{code}")
    public Result<Product> getByCode(@PathVariable String code) {
        Product product = productService.lambdaQuery()
                .eq(Product::getProductCode, code)
                .one();
        if (product == null) {
            return Result.error("商品不存在");
        }
        return Result.success(product);
    }

    /**
     * 查询商品树形结构（按 product_name 分组）
     * 请求方式: GET
     * 请求路径: /api/product/tree
     *
     * 返回数据结构：
     * [
     *   {
     *     "productName": "白针织42#",
     *     "children": [
     *       { "id": 1, "productCode": "BZZ-42-19", "productName": "白针织42#", "specification": "1.9mm/6分", "unit": "公斤", "unitPrice": 5.5 },
     *       ...
     *     ]
     *   },
     *   ...
     * ]
     */
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree() {
        List<Product> products = productService.lambdaQuery()
                .orderByAsc(Product::getProductName)
                .orderByAsc(Product::getId)
                .list();

        // 按 productName 分组
        Map<String, List<Product>> groupMap = products.stream()
                .collect(Collectors.groupingBy(Product::getProductName));

        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map.Entry<String, List<Product>> entry : groupMap.entrySet()) {
            Map<String, Object> node = new HashMap<>();
            node.put("productName", entry.getKey());
            node.put("children", entry.getValue());
            tree.add(node);
        }

        return Result.success(tree);
    }
}
