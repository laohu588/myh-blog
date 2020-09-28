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
            "execution( * com.myh.blog.repository.*.select*(..))||" +
            "execution( * com.myh.blog.repository.*.query*(..))||" +
            "execution( * com.myh.blog.repository.*.get*(..))||"+
            "execution( * com.myh.blog.repository.*.count*(..))")

    public void setSlaveDataSource() {
        DataSourceContextHolder.set(SLAVE);
    }
    /**
     *  对包含Master注解的方法或者是mapper中的方法是insert,
     *  update,delete开头的方法设置为主库
     */
    @Before("@annotation(club.javafan.blog.common.annotation.Master)||" +
            "execution( * com.myh.blog.repository.*.insert*(..))||" +
            "execution( * com.myh.blog.repository.*.update*(..))||" +
            "execution( * com.myh.blog.repository.*.delete*(..))||" +
            "execution( * com.myh.blog.repository.*.add*(..))")
    public void setMasterDataSource() {
        DataSourceContextHolder.set(MASTER);
    }
}