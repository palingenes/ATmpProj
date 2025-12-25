package com.wzy.testunity.tool;

import androidx.annotation.NonNull;

import com.wzy.testunity.bean.ChapterBuffer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NovelParser {

    // 匹配章节标题：
    // - "序章 ..."
    // - "第123章 ..."
    // - "第一章 ..."
    // - "第一千零二十四章 ..."
    private static final Pattern CHAPTER_PATTERN =
            Pattern.compile("^(序章|第([\\d一二三四五六七八九十百千万零两]+)章)\\s*(.*)$");

    public static List<ChapterBuffer> parseNovel(String filePath) throws IOException {
        List<ChapterBuffer> chapters = new ArrayList<>();
        ChapterBuffer currentChapter = null;
        List<String> currentLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    continue;
                }
                Matcher matcher = CHAPTER_PATTERN.matcher(trimmedLine);
                if (matcher.matches()) {
                    // 保存上一章
                    if (currentChapter != null) {
                        currentChapter.content = new ArrayList<>(currentLines);
                        chapters.add(currentChapter);
                        currentLines.clear();
                    }

                    String fullMatch = matcher.group(1); // "序章" 或 "第...章"
                    String numberStr = matcher.group(2); // null 表示是“序章”
                    String titlePart = Objects.requireNonNull(matcher.group(3)).trim();

                    int number;
                    String displayName;

                    if ("序章".equals(fullMatch)) {
                        number = 0; // 序章排最前
                        displayName = "序章 " + titlePart;
                    } else {
                        // 是“第...章”格式
                        try {
                            if (Objects.requireNonNull(numberStr).matches("\\d+")) {
                                number = Integer.parseInt(numberStr);
                            } else {
                                number = chineseToNumber(numberStr);
                            }
                        } catch (Exception e) {
                            System.err.println("⚠️ 无法解析章节编号: \"" + numberStr + "\"，跳过该章");
                            number = Integer.MAX_VALUE;
                        }
                        displayName = "第" + numberStr + "章 " + titlePart;
                    }

                    currentChapter = new ChapterBuffer();
                    currentChapter.number = number;
                    currentChapter.name = displayName;
                } else {
                    if (currentChapter != null) {
                        currentLines.add(line);
                    }
                }
            }

            // 添加最后一章
            if (currentChapter != null) {
                currentChapter.content = new ArrayList<>(currentLines);
                chapters.add(currentChapter);
            }
        }

        // 按章节号升序排序（序章=0，正文从1开始）
        chapters.sort(Comparator.comparingInt(ch -> ch.number));
        return chapters;
    }

    /**
     * 将中文数字（含“两”）转换为阿拉伯数字，范围 1 ~ 9999
     * 支持：一、二、两、十、一百、两千、九千九百九十九 等
     */
    private static int chineseToNumber(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            throw new NumberFormatException("Empty input");
        }

        // 统一处理：“两” → “二”
        chinese = chinese.replace("两", "二").replace("零", "");

        Map<Character, Integer> digitMap = new HashMap<>();
        digitMap.put('一', 1);
        digitMap.put('二', 2);
        digitMap.put('三', 3);
        digitMap.put('四', 4);
        digitMap.put('五', 5);
        digitMap.put('六', 6);
        digitMap.put('七', 7);
        digitMap.put('八', 8);
        digitMap.put('九', 9);

        int result = 0;
        int currentSection = 0;

        for (int i = 0; i < chinese.length(); i++) {
            char c = chinese.charAt(i);
            if (c == '千') {
                int multiplier = (currentSection == 0) ? 1 : currentSection;
                result += multiplier * 1000;
                currentSection = 0;
            } else if (c == '百') {
                int multiplier = (currentSection == 0) ? 1 : currentSection;
                currentSection = multiplier * 100;
            } else if (c == '十') {
                int multiplier = (currentSection == 0) ? 1 : currentSection;
                currentSection = multiplier * 10;
            } else {
                Integer digit = digitMap.get(c);
                if (digit == null) {
                    throw new NumberFormatException("Invalid Chinese character: " + c);
                }
                currentSection += digit;
            }
        }

        result += currentSection;

        if (result <= 0 || result > 9999) {
            throw new NumberFormatException("Out of supported range (1-9999): " + result);
        }

        return result;
    }
}