package club.javafan.blog.common.annotation;

import club.javafan.blog.domain.enums.RedisStructureEnum;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface RedisKey {
    //redis key 的描述
    String desc();
    //redis 的数据结构
    RedisStructureEnum structure();
}
