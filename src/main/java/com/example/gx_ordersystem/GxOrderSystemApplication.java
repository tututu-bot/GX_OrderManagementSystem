package com.example.gx_ordersystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.gx_ordersystem.mapper")
public class GxOrderSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(GxOrderSystemApplication.class, args);
    }

}
