package com.wzy.testunity.source;


import com.wzy.testunity.bean.Chapter;
import com.wzy.testunity.bean.ChapterBuffer;
import com.wzy.testunity.engine.FastDownloader;
import com.wzy.testunity.util.RegexUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created By zia on 2018/10/5.
 * 测试可达到4.5m/s
 */
public class BiQuGe extends FastDownloader {

    private final static String root = "http://www.biquge.com.tw";

    public BiQuGe(String bookName, String path) {
        super(bookName, getUrl(bookName), path);
    }

    public BiQuGe(String bookName, String catalogUrl, String path) {
        super(bookName, catalogUrl, path);
    }

    private static String getUrl(String bookName) {
        try {
            return root + "/modules/article/soshu.php?searchkey=+"
                    + URLEncoder.encode(bookName, "GBK");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public List<Chapter> getChapters(String catalogUrl) throws IOException {
        String catalogHTML = getHtml(catalogUrl);
        List<String> as = RegexUtil.regexExcept("<dd>", "</dd>", catalogHTML);
        List<Chapter> chapters = new ArrayList<>(10000);
        for (String a : as) {
            Chapter chapter = new Chapter();
            chapter.name = RegexUtil.regexExcept("\">", "</a>", a).get(0);
            chapter.href = root + RegexUtil.regexExcept("<a href=\"", "\">", a).get(0);
            chapters.add(chapter);
        }
        return chapters;
    }

    @Override
    public ChapterBuffer adaptBookBuffer(Chapter chapter, int num) throws IOException {
        String html = getHtml(chapter.getHref());
        List<String> lines = RegexUtil.regexExcept("&nbsp;&nbsp;&nbsp;&nbsp;", "<br />", html);
        List<String> contents = new ArrayList<>();

        lines.forEach(s -> {
            if (!s.trim().isEmpty()){
                contents.add(cleanContent(s));
            }
        });

        ChapterBuffer chapterBuffer = new ChapterBuffer();
        chapterBuffer.content = contents;
        chapterBuffer.name = chapter.name;
        chapterBuffer.number = num;
        return chapterBuffer;
    }
}
