package club.javafan.blog.common.util;

import java.net.URI;


public class BlogUtils {

    /**
     * 获取URI
     *
     * @param uri
     * @return
     * @throws Exception
     */
    public static URI getHost(URI uri) throws Exception {
        URI effectiveURI = new URI(uri.getScheme(), uri.getUserInfo()
                , uri.getHost(), uri.getPort(), null, null, null);
        return effectiveURI;
    }

}