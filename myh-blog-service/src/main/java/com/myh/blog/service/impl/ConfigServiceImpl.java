package com.myh.blog.service.impl;


import club.javafan.blog.common.util.RedisUtil;
import club.javafan.blog.domain.Config;
import club.javafan.blog.domain.example.ConfigExample;
import club.javafan.blog.repository.ConfigMapper;
import com.myh.blog.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static club.javafan.blog.common.constant.RedisKeyConstant.BLOG_INDEX_VIEW;
import static club.javafan.blog.common.constant.RedisKeyConstant.BLOG_INDEX_VIEW_ALL;
import static club.javafan.blog.common.util.DateUtils.getDistanceToNow;
import static club.javafan.blog.common.util.DateUtils.getToday;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;


@Service
public class ConfigServiceImpl implements ConfigService {
    /**
     * 配置项查询
     */
    @Autowired
    private ConfigMapper configMapper;
    /**
     * redis 查询
     */
    @Autowired
    private RedisUtil redisUtil;
    @Value("${blog.run.date}")
    private String runDate;
    public static final String WEBSITE_NAME = "个人博客";
    public static final String WEBSITE_DESC = "敲代码的长腿毛欧巴是SpringBoot2+Thymeleaf+Mybatis建造的个人博客网站.SpringBoot实战博客源码.个人博客搭建";
    public static final String WEBSITE_LOGO = "/admin/dist/img/logo2.png";
    public static final String WEBSITE_ICON = "/admin/dist/img/favicon.png";

    public static final String AVATAR = "/admin/dist/img/13.png";
    public static final String EMAIL = "renjiahua945@vip.qq.com";
    public static final String NAME = "敲代码的长腿毛欧巴";

    public static final String ABOUT = "世上本没有bug,写的人多了也便成了bug";
    public static final String ICP = "冀ICP备 xxxxxx-x号";
    public static final String COPY_RIGHT = "@2020 敲代码的长腿毛欧巴";
    public static final String POWER_BY = "敲代码的长腿毛欧巴";
    public static final String POWER_BY_URL = "##";

    @Override
    public int updateConfig(String configName, String configValue) {
        Config blogConfig = configMapper.selectByPrimaryKey(configName);
        if (Objects.nonNull(blogConfig)) {
            blogConfig.setConfigValue(configValue);
            blogConfig.setUpdateTime(new Date());
            return configMapper.updateByPrimaryKeySelective(blogConfig);
        }
        return INTEGER_ZERO;
    }

    @Override
    public Map<String, String> getAllConfigs() throws Exception {
        //获取所有的map并封装为map
        ConfigExample example = new ConfigExample();
        List<Config> blogConfigs = configMapper.selectByExample(example);
        Map<String, String> configMap = blogConfigs.stream().collect(Collectors.toMap(Config::getConfigName, Config::getConfigValue));
        for (Map.Entry<String, String> config : configMap.entrySet()) {
            if ("WEBSITE_NAME".equals(config.getKey()) && StringUtils.isEmpty(config.getValue())) {
                config.setValue(WEBSITE_NAME);
            }
            if ("WEBSITE_DESC".equals(config.getKey()) && StringUtils.isEmpty(config.getValue())) {
                config.setValue(WEBSITE_DESC);
            }
            if ("WEBSITE_LOGO".equals(config.getKey()) && StringUtils.isEmpty(config.getValue())) {
                config.setValue(WEBSITE_LOGO);
            }
            if ("WEBSITE_ICON".equals(config.getKey()) && StringUtils.isEmpty(config.getValue())) {
                config.setValue(WEBSITE_ICON);
            }
            if ("AVATAR".equals(config.getKey()) && StringUtils.isEmpty(config.getValue())) {
                config.setValue(AVATAR);
            }
            if ("EMAIL".equals(config.getKey()) && StringUtils.isEmpty(config.getValue())) {
                config.setValue(EMAIL);
            }
            if ("NAME".equals(config.getKey()) && StringUtils.isEmpty(config.getValue())) {
                config.setValue(NAME);
            }
            if ("ABOUT".equals(config.getKey()) && StringUtils.isEmpty(config.getValue())) {
                config.setValue(ABOUT);
            }
            if ("ICP".equals(config.getKey()) && StringUtils.isEmpty(config.getValue())) {
                config.setValue(ICP);
            }
            if ("COPY_RIGHT".equals(config.getKey()) && StringUtils.isEmpty(config.getValue())) {
                config.setValue(COPY_RIGHT);
            }
            if ("POWER_BY".equals(config.getKey()) && StringUtils.isEmpty(config.getValue())) {
                config.setValue(POWER_BY);
            }
            if ("POWER_BY_URL".equals(config.getKey()) && StringUtils.isEmpty(config.getValue())) {
                config.setValue(POWER_BY_URL);
            }
        }
        //获取今天访客数
        Object indexViewToday = redisUtil.get(BLOG_INDEX_VIEW + getToday());
        configMap.put("indexViewToday", String.valueOf(indexViewToday));
        //获取总的访客数
        Object indexViewAll = redisUtil.get(BLOG_INDEX_VIEW_ALL);
        configMap.put("indexViewAll", String.valueOf(indexViewAll));
        //获取服务器的运行天数
        configMap.put("runDays", getDistanceToNow(runDate).toString());

        return configMap;
    }
}
