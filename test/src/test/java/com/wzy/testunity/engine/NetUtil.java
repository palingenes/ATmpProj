package com.wzy.testunity.engine;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

/**
 * Created By zia on 2018/10/21.
 */
public class NetUtil {
    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            //请设置代理，否则会被小说网站ban的...量小没关系
//            .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("106.75.226.36", 808)))
            .build();

    /**
     * 同步获取html文件，默认编码gbk
     */
    public static String getHtml(String url) throws IOException {
        return getHtml(url, /*"gbk"*/"UTF-8");
    }

    public static String getHtml(String url, String encodeType) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("accept", "*/*")
                .addHeader("connection", "Keep-Alive")
                .addHeader("Charsert", "UTF-8")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36")
                .build();
        ResponseBody body = okHttpClient.newCall(request).execute().body();
        return new String(body.bytes(), encodeType);
    }
}
