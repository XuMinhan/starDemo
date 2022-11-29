package com.example.starqiangloudemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.example.starqiangloudemo.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class StarQianglouDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(StarQianglouDemoApplication.class, args);
    }

}
