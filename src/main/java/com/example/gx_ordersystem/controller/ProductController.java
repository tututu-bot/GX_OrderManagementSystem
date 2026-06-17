package com.example.gx_ordersystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.gx_ordersystem.common.Result;
import com.example.gx_ordersystem.entity.Product;
import com.example.gx_ordersystem.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    /** 查询当前用户的商品列表（分页） */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(defaultValue = "") String keyword,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        var query = productService.lambdaQuery()
                .eq(Product::getUserId, userId);
        if (keyword != null && !keyword.isBlank()) {
            query.and(w -> w.like(Product::getProductName, keyword)
                    .or().like(Product::getProductCode, keyword)
                    .or().like(Product::getSpecification, keyword));
        }
        query.orderByAsc(Product::getProductName).orderByAsc(Product::getId);
        IPage<Product> page = query.page(new Page<>(pageNum, pageSize));

        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        return Result.success(result);
    }

    /** 查询当前用户的全部商品（树形选择用） */
    @GetMapping
    public Result<List<Product>> list(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return Result.success(productService.lambdaQuery()
                .eq(Product::getUserId, userId)
                .orderByAsc(Product::getProductName)
                .orderByAsc(Product::getId)
                .list());
    }

    /** 根据编码查询商品 */
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

    /** 新增商品，编码为空时自动生成 */
    @PostMapping
    public Result<Product> save(@RequestBody Product product, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        product.setUserId(userId);

        // 检查该用户是否已添加过同名同规格商品
        long dupCount = productService.lambdaQuery()
                .eq(Product::getUserId, userId)
                .eq(Product::getProductName, product.getProductName())
                .eq(Product::getSpecification, product.getSpecification())
                .count();
        if (dupCount > 0) {
            return Result.error("该商品已存在");
        }

        // 编码为空时自动生成随机编码
        if (product.getProductCode() == null || product.getProductCode().isBlank()) {
            product.setProductCode(generateProductCode());
        }

        // 编码唯一性校验
        long codeCount = productService.lambdaQuery()
                .eq(Product::getProductCode, product.getProductCode())
                .count();
        if (codeCount > 0) {
            return Result.error("商品编码 " + product.getProductCode() + " 已存在");
        }

        productService.save(product);
        return Result.success(product);
    }

    private String generateProductCode() {
        return "SP" + (100000000 + new Random().nextInt(900000000));
    }

    /** 修改商品，校验归属 */
    @PutMapping
    public Result<Product> update(@RequestBody Product product, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Product exist = productService.getById(product.getId());
        if (exist == null) {
            return Result.error("商品不存在");
        }
        if (!userId.equals(exist.getUserId())) {
            return Result.error("无权修改此商品");
        }
        product.setUserId(userId);
        productService.updateById(product);
        return Result.success(product);
    }

    /** 删除商品，校验归属 */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Product exist = productService.getById(id);
        if (exist == null) {
            return Result.error("商品不存在");
        }
        if (!userId.equals(exist.getUserId())) {
            return Result.error("无权删除此商品");
        }
        productService.removeById(id);
        return Result.success("删除成功");
    }

    /** 批量导入 CSV */
    @PostMapping("/import")
    public Result<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file,
                                                  HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        int success = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            // 跳过 BOM
            reader.mark(1);
            int bom = reader.read();
            if (bom != 0xFEFF) reader.reset();

            // 读取表头
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return Result.error("文件为空");
            }
            // 跳过表头，逐行解析
            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) continue;
                try {
                    Product p = parseCsvRow(line);
                    if (p == null) {
                        errors.add("第" + rowNum + "行：数据格式错误");
                        continue;
                    }
                    p.setUserId(userId);

                    // 检查该用户是否已添加过同名同规格商品
                    long dupCount = productService.lambdaQuery()
                            .eq(Product::getUserId, userId)
                            .eq(Product::getProductName, p.getProductName())
                            .eq(Product::getSpecification, p.getSpecification())
                            .count();
                    if (dupCount > 0) {
                        errors.add("第" + rowNum + "行：商品已存在");
                        continue;
                    }

                    // 编码自动生成
                    if (p.getProductCode() == null || p.getProductCode().isBlank()) {
                        p.setProductCode(generateProductCode());
                    }

                    // 编码唯一性校验
                    long codeCount = productService.lambdaQuery()
                            .eq(Product::getProductCode, p.getProductCode())
                            .count();
                    if (codeCount > 0) {
                        errors.add("第" + rowNum + "行：编码 " + p.getProductCode() + " 已存在");
                        continue;
                    }

                    productService.save(p);
                    success++;
                } catch (Exception e) {
                    errors.add("第" + rowNum + "行：" + e.getMessage());
                }
            }
        } catch (Exception e) {
            return Result.error("文件读取失败：" + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("errors", errors.size());
        if (!errors.isEmpty()) {
            result.put("errorMsg", String.join("\n", errors.size() > 10
                    ? errors.subList(0, 10) : errors)
                    + (errors.size() > 10 ? "\n...共 " + errors.size() + " 条错误" : ""));
        } else {
            result.put("errorMsg", "");
        }
        return Result.success(result);
    }

    /** 解析 CSV 一行：product_name,color,size,yard_code,unit,unit_price,remark */
    private Product parseCsvRow(String line) {
        List<String> fields = parseCsvFields(line);
        if (fields.size() < 6) return null;

        Product p = new Product();
        p.setProductName(fields.get(0).trim());
        String color = fields.get(1).trim();
        String size = fields.get(2).trim();
        String yardCode = fields.size() > 3 ? fields.get(3).trim() : "";
        p.setUnit(fields.size() > 4 && !fields.get(4).isBlank() ? fields.get(4).trim() : "公斤");
        try {
            p.setUnitPrice(fields.size() > 5 && !fields.get(5).isBlank()
                    ? new BigDecimal(fields.get(5).trim()) : BigDecimal.ZERO);
        } catch (NumberFormatException e) {
            p.setUnitPrice(BigDecimal.ZERO);
        }
        p.setRemark(fields.size() > 6 && !fields.get(6).isBlank() ? fields.get(6).trim() : null);

        // 组装 specification
        String spec = color + ";" + size;
        if (!yardCode.isBlank()) spec += ";" + yardCode;
        p.setSpecification(spec);

        return p;
    }

    /** 简易 CSV 字段解析，处理引号 */
    private List<String> parseCsvFields(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());
        return fields;
    }

    /**
     * 三级商品树：product_name → 颜色 → 商品列表
     */
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<Product> products = productService.lambdaQuery()
                .eq(Product::getUserId, userId)
                .orderByAsc(Product::getProductName)
                .orderByAsc(Product::getId)
                .list();

        Map<String, List<Product>> nameGroupMap = products.stream()
                .collect(Collectors.groupingBy(Product::getProductName, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map.Entry<String, List<Product>> nameEntry : nameGroupMap.entrySet()) {
            Map<String, Object> nameNode = new LinkedHashMap<>();
            nameNode.put("productName", nameEntry.getKey());

            Map<String, List<Product>> colorGroupMap = nameEntry.getValue().stream()
                    .collect(Collectors.groupingBy(p -> {
                        String spec = p.getSpecification();
                        if (spec == null) return "";
                        int idx = spec.indexOf(';');
                        return idx > 0 ? spec.substring(0, idx) : spec;
                    }, LinkedHashMap::new, Collectors.toList()));

            List<Map<String, Object>> colors = new ArrayList<>();
            for (Map.Entry<String, List<Product>> colorEntry : colorGroupMap.entrySet()) {
                Map<String, Object> colorNode = new LinkedHashMap<>();
                colorNode.put("color", colorEntry.getKey());
                colorNode.put("children", colorEntry.getValue());
                colors.add(colorNode);
            }
            nameNode.put("colors", colors);
            tree.add(nameNode);
        }

        return Result.success(tree);
    }
}
