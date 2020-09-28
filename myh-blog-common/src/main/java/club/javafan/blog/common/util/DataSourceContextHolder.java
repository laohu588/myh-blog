package club.javafan.blog.common.util;

import club.javafan.blog.domain.enums.DataSourceEnum;


public class DataSourceContextHolder {
    /**
     * 数据源context
     */
    private static ThreadLocal<DataSourceEnum> context = new ThreadLocal<>();

    /**
     * 获取当前的数据源
     *
     * @return DataSourceEnum
     */
    public static DataSourceEnum get() {
        return context.get();
    }

    /**
     * 设置当前的数据源
     *
     * @param dataSourceEnum
     */
    public static void set(DataSourceEnum dataSourceEnum) {
        context.set(dataSourceEnum);
    }

    /**
     * 移除
     */
    public static void remove() {
        context.remove();
    }
}