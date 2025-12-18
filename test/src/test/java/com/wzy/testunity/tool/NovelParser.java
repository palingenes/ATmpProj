package com.wzy.testunity.tool;

import com.wzy.testunity.bean.ChapterBuffer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NovelParser {

    // 匹配章节标题的正则：例如 "第12章 私活" 或 "第259章血肉畸变体"
    private static final Pattern CHAPTER_PATTERN =
            Pattern.compile("^第(\\d+)章\\s*(.+)$");

    public static List<ChapterBuffer> parseNovel(String filePath) throws IOException {
        List<ChapterBuffer> chapters = new ArrayList<>();
        ChapterBuffer currentChapter = null;
        List<String> currentLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String originalLine = line; // 保留原始行（含空格）
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                Matcher matcher = CHAPTER_PATTERN.matcher(line);
                if (matcher.matches()) {
                    if (currentChapter != null) {
                        currentChapter.content = new ArrayList<>(currentLines);
                        chapters.add(currentChapter);
                        currentLines.clear();
                    }

                    int number = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
                    currentChapter = new ChapterBuffer();
                    currentChapter.number = number;
                    currentChapter.name = line;
                } else {
                    if (currentChapter != null) {
                        currentLines.add(originalLine); // 可选：保留原始行（含缩进/空格）
                        // 或者用 line（已 trim）：currentLines.add(line);
                    }
                }
            }
            if (currentChapter != null) {
                currentChapter.content = new ArrayList<>(currentLines);
                chapters.add(currentChapter);
            }
        }
        chapters.sort(Comparator.comparingInt(ch -> ch.number));
        return chapters;
    }
}
