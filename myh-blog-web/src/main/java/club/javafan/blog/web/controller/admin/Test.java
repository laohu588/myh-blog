package club.javafan.blog.web.controller.admin;

import club.javafan.blog.common.util.MD5Util;

public class Test {
    public static void main(String[] args) {
        String passwordMd5 = MD5Util.md5Encode("admin", "UTF-8");
        System.out.println(passwordMd5);
    }
}