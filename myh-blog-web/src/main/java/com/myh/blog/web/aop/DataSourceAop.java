package com.myh.blog.web.aop;

import club.javafan.blog.common.util.DataSourceContextHolder;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import static club.javafan.blog.domain.enums.DataSourceEnum.MASTER;
import static club.javafan.blog.domain.enums.DataSourceEnum.SLAVE;

@Aspect
@Component
public class DataSourceAop {


    @Before("@annotation(club.javafan.blog.common.annotation.Slave)||" +
            "execution( * club.javafan.blog.repository.*.select*(..))||" +
            "execution( * club.javafan.blog.repository.*.query*(..))||" +
            "execution( * club.javafan.blog.repository.*.get*(..))||"+
            "execution( * club.javafan.blog.repository.*.count*(..))")

    public void setSlaveDataSource() {
        DataSourceContextHolder.set(SLAVE);
    }
    /**
     *  对包含Master注解的方法或者是mapper中的方法是insert,
     *  update,delete开头的方法设置为主库
     */
    @Before("@annotation(club.javafan.blog.common.annotation.Master)||" +
            "execution( * club.javafan.blog.repository.*.insert*(..))||" +
            "execution( * club.javafan.blog.repository.*.update*(..))||" +
            "execution( * club.javafan.blog.repository.*.delete*(..))||" +
            "execution( * club.javafan.blog.repository.*.add*(..))")
    public void setMasterDataSource() {
        DataSourceContextHolder.set(MASTER);
    }
}