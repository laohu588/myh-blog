package com.myh.blog.web;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.myh.blog.repository")
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan({
        "com.myh.blog.service"
        , "com.myh.blog.web.controller"
        , "com.myh.blog.web.aop"
        , "com.myh.blog.web.filter"
        , "club.javafan.blog.configs"
        , "club.javafan.blog.common.util"
        , "club.javafan.blog.common.mail.impl"
        , "club.javafan.blog.common.threadpool"
        , "club.javafan.blog.common.qquserinfo"
        , "club.javafan.blog.worker"
})
@Slf4j
public class BlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
        log.info(">>> myh blog start success...");
    }

}
