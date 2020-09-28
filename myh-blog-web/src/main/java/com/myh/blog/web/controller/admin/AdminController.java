package com.myh.blog.web.controller.admin;


import club.javafan.blog.common.constant.RedisKeyConstant;
import club.javafan.blog.common.result.ResponseResult;
import club.javafan.blog.common.util.CookiesUtil;
import club.javafan.blog.common.util.MD5Util;
import club.javafan.blog.common.util.RedisUtil;
import club.javafan.blog.domain.AdminUser;
import com.myh.blog.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;

import static club.javafan.blog.common.constant.CommonConstant.COOKIES;
import static club.javafan.blog.common.constant.CommonConstant.LOGIN_USER_NAME;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Controller
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminUserService adminUserService;
    @Resource
    private BlogService blogService;
    @Resource
    private CategoryService categoryService;
    @Resource
    private LinkService linkService;
    @Resource
    private TagService tagService;
    @Resource
    private CommentService commentService;
    @Resource
    private RedisUtil redisUtil;

    @GetMapping({"/login"})
    public ModelAndView login() {
        return new ModelAndView("admin/login");
    }

    @GetMapping({"/test"})
    public ModelAndView test() {
        return new ModelAndView("admin/test");
    }


    @GetMapping({"", "/", "/index", "/index.html"})
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView("admin/index");
        modelAndView.addObject("path", "index");
        modelAndView.addObject("categoryCount", categoryService.getTotalCategories());
        modelAndView.addObject("blogCount", blogService.getTotalBlogs());
        modelAndView.addObject("linkCount", linkService.getTotalLinks());
        modelAndView.addObject("tagCount", tagService.getTotalTags());
        modelAndView.addObject("commentCount", commentService.getTotalComments());
        modelAndView.addObject("path", "index");
        return modelAndView;
    }

    @PostMapping(value = "/login")
    public ModelAndView login(@RequestParam("userName") String userName,
                              @RequestParam("password") String password,
                              @RequestParam("verifyCode") String verifyCode,
                              HttpSession session,
                              HttpServletResponse response,
                              HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView("admin/login");
        if (StringUtils.isEmpty(verifyCode)) {
            modelAndView.addObject("errorMsg", "验证码不能为空！");
            return modelAndView;
        }
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password)) {
            modelAndView.addObject("errorMsg", "用户名或密码不能为空！");
            return modelAndView;
        }
        String kaptchaCode = session.getAttribute("verifyCode") + "";
        System.out.println(kaptchaCode);
        if (StringUtils.isEmpty(kaptchaCode) || !verifyCode.equals(kaptchaCode)) {
            modelAndView.addObject("errorMsg", "验证码错误！");
            return modelAndView;
        }
        AdminUser adminUser = adminUserService.login(userName, password);
        if (nonNull(adminUser)) {
            setCookies(adminUser, response);
            modelAndView.addObject("adminUser", adminUser);
            return new ModelAndView("redirect:/admin/index");
        }
        modelAndView.addObject("errorMsg", "登录失败！");
        return modelAndView;

    }


    @GetMapping("/profile")
    public ModelAndView profile(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView("admin/login");
        Cookie cookie = CookiesUtil.getCookie(LOGIN_USER_NAME, request.getCookies());
        if (isNull(cookie)) {
            return modelAndView;
        }
        AdminUser adminUser = adminUserService.getUserDetailByUserName(cookie.getValue());
        if (isNull(adminUser)) {
            return modelAndView;
        }
        modelAndView.setViewName("admin/profile");
        modelAndView.addObject("path", "profile");
        modelAndView.addObject("loginUserName", adminUser.getLoginUserName());
        modelAndView.addObject("nickName", adminUser.getNickName());
        return modelAndView;
    }

    @PostMapping("/profile/password")
    @ResponseBody
    public ResponseResult passwordUpdate(HttpServletRequest request,
                                         @RequestParam("originalPassword") String originalPassword,
                                         @RequestParam("newPassword") String newPassword) {
        if (StringUtils.isEmpty(originalPassword) || StringUtils.isEmpty(newPassword)) {
            return ResponseResult.failResult("参数不能为空");
        }
        Cookie cookie = CookiesUtil.getCookie(LOGIN_USER_NAME, request.getCookies());
        if (isNull(cookie)) {
            return ResponseResult.failResult("修改失败！");
        }
        AdminUser adminUser = adminUserService.getUserDetailByUserName(cookie.getValue());
        if (isNull(adminUser)) {
            return ResponseResult.failResult("修改失败！");
        }
        ResponseResult responseResult = adminUserService.updatePassword(adminUser.getAdminUserId(),
                originalPassword, newPassword);
        return responseResult;
    }

    @PostMapping("/profile/name")
    @ResponseBody
    public ResponseResult nameUpdate(HttpServletRequest request,
                                     @RequestParam("loginUserName") String loginUserName,
                                     @RequestParam("nickName") String nickName) {
        Cookie cookie = CookiesUtil.getCookie(LOGIN_USER_NAME, request.getCookies());
        if (isNull(cookie)) {
            return ResponseResult.failResult("修改失败！");
        }
        AdminUser adminUser = adminUserService.getUserDetailByUserName(cookie.getValue());
        if (StringUtils.isEmpty(loginUserName) || StringUtils.isEmpty(nickName) || isNull(adminUser)) {
            return ResponseResult.failResult("参数不能为空");
        }
        ResponseResult responseResult = adminUserService.updateName(adminUser.getAdminUserId(), loginUserName, nickName);
        return responseResult;
    }

    @GetMapping("/logout")
    public ModelAndView logout(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIES, "");
        cookie.setMaxAge(0);
        Cookie useCookie = new Cookie(LOGIN_USER_NAME, "");
        useCookie.setMaxAge(0);
        response.addCookie(cookie);
        response.addCookie(useCookie);
        return new ModelAndView("admin/login");
    }

    /**
     * 设置登录的cookies
     *
     * @param adminUser 用户
     * @param response  res
     */
    private void setCookies(AdminUser adminUser, HttpServletResponse response) {
        //加密
        String origin = adminUser.getLoginUserName() + adminUser.getAdminUserId() + adminUser.getLoginPassword() + new Date();
        String md5 = MD5Util.md5Encode(origin, "UTF-8");
        redisUtil.set(RedisKeyConstant.BLOG_SESSION + adminUser.getLoginUserName(), md5, 30 * 60);
        Cookie cookie = new Cookie(COOKIES, md5);
        //设置有效时间半个小时
        cookie.setMaxAge(30 * 60);
        //设置为true 避免抓包
        cookie.setHttpOnly(true);
        Cookie useCookie = new Cookie(LOGIN_USER_NAME, adminUser.getLoginUserName());
        useCookie.setMaxAge(30 * 60);
        response.addCookie(cookie);
        response.addCookie(useCookie);
    }
}
