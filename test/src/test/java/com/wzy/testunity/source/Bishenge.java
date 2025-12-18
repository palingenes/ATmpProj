package com.wzy.testunity.source;


import com.wzy.testunity.bean.Chapter;
import com.wzy.testunity.bean.ChapterBuffer;
import com.wzy.testunity.engine.FastDownloader;
import com.wzy.testunity.util.RegexUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created By zia on 2018/10/5.
 * 测试约1.5m/s
 */
public class Bishenge extends FastDownloader {

    public Bishenge(String bookName, String catalogUrl, String path) {
        super(bookName, catalogUrl, path);
    }

    @Override
    public List<Chapter> getChapters(String catalogUrl) throws IOException {
        String catalogHTML = getHtml(catalogUrl);
        String sub = RegexUtil.regexExcept("<div id=\"list\">", "</div>", catalogHTML).get(0);
        String[] split = sub.split("正文</dt>");
        if (split.length <= 1) return null;
        String ssub = split[1];
        List<String> as = RegexUtil.regexInclude("<a", "</a>", ssub);
        List<Chapter> list = new ArrayList<>();
        as.forEach(s -> {
            RegexUtil.Tag tag = new RegexUtil.Tag(s);
            Chapter chapter = new Chapter();
            chapter.name = tag.getText();
            chapter.href = catalogUrl + tag.getValue("href");
            list.add(chapter);
        });
        return list;
    }

    @Override
    public ChapterBuffer adaptBookBuffer(Chapter chapter, int num) throws IOException {
        ChapterBuffer chapterBuffer = new ChapterBuffer();
        chapterBuffer.number = num;
        chapterBuffer.name = chapter.name;

        String html = getHtml(chapter.href);

        String sub = RegexUtil.regexExcept("<div id=\"content\">", "</div>", html).get(0);

        String lines[] = sub.split("<br>|<br/>|<br />");

        List<String> content = new ArrayList<>();

        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                content.add(cleanContent(line));
            }
        }
        chapterBuffer.content = content;
        return chapterBuffer;
    }
}
