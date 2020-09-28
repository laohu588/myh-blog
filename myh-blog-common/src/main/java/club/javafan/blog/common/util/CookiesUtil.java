package club.javafan.blog.common.util;

import org.apache.commons.lang3.ArrayUtils;

import javax.servlet.http.Cookie;
import java.util.Optional;
import java.util.stream.Stream;


public class CookiesUtil {
    /**
     * 获取指定key的cookies
     *
     * @param key
     * @param cookies
     * @return
     */
    public static Cookie getCookie(String key, Cookie[] cookies) {
        if (ArrayUtils.isEmpty(cookies)) {
            return null;
        }
        Optional<Cookie> optional = Stream.of(cookies).filter(cookie -> key.equals(cookie.getName())).findFirst();
        return optional.isPresent() ? optional.get() : null;
    }

}
