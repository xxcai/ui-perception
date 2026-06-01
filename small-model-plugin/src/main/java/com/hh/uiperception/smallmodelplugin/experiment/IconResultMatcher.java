package com.hh.uiperception.smallmodelplugin.experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 将模型输出与 target 预期自动匹配，计算准确率。
 */
public final class IconResultMatcher {

    private IconResultMatcher() {
    }

    public static List<IconMatchResult> match(
            List<ParsedIconDescription> parsedOutput,
            List<IconTarget> targets
    ) {
        List<IconMatchResult> results = new ArrayList<>();
        if (targets == null) {
            return results;
        }
        for (IconTarget target : targets) {
            String actual = findDesc(parsedOutput, target.id());
            boolean matched = isMatch(actual, target);
            results.add(new IconMatchResult(
                    target.id(),
                    target.expected(),
                    actual,
                    matched
            ));
        }
        return results;
    }

    public static int matchCount(List<IconMatchResult> results) {
        int count = 0;
        for (IconMatchResult result : results) {
            if (result.matched()) {
                count++;
            }
        }
        return count;
    }

    public static String formatResultTable(List<IconMatchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int matched = 0;
        for (IconMatchResult result : results) {
            builder.append(result.id())
                    .append("  ")
                    .append(padRight(result.expected(), 12))
                    .append("  ")
                    .append(padRight(result.actual().isEmpty() ? "-" : result.actual(), 12))
                    .append("  ")
                    .append(result.matched() ? "OK" : "X")
                    .append("\n");
            if (result.matched()) {
                matched++;
            }
        }
        builder.append("---\n")
                .append(matched).append("/").append(results.size())
                .append("  ")
                .append(String.format("%.0f%%", results.isEmpty() ? 0 : matched * 100.0 / results.size()));
        return builder.toString();
    }

    private static String findDesc(List<ParsedIconDescription> parsedOutput, String id) {
        if (parsedOutput == null || id == null) {
            return "";
        }
        for (ParsedIconDescription item : parsedOutput) {
            if (id.equalsIgnoreCase(item.id())) {
                return item.desc();
            }
        }
        return "";
    }

    private static boolean isMatch(String actual, IconTarget target) {
        if (actual == null || actual.isEmpty()) {
            return false;
        }
        String normalizedActual = normalize(actual);
        if (containsNormalized(normalizedActual, target.expected())) {
            return true;
        }
        if (target.acceptable() != null) {
            for (String alt : target.acceptable()) {
                if (containsNormalized(normalizedActual, alt)) {
                    return true;
                }
            }
        }
        for (List<String> group : semanticGroups(target)) {
            for (String keyword : group) {
                if (containsNormalized(normalizedActual, keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsNormalized(String normalizedActual, String expected) {
        String normalizedExpected = normalize(expected);
        if (normalizedActual.isEmpty() || normalizedExpected.isEmpty()) {
            return false;
        }
        return normalizedActual.contains(normalizedExpected)
                || normalizedExpected.contains(normalizedActual);
    }

    private static List<List<String>> semanticGroups(IconTarget target) {
        List<List<String>> groups = new ArrayList<>();
        if (target == null) {
            return groups;
        }
        addGroupIfTargetMatches(groups, target,
                "头像", "个人", "用户", "人像", "人头", "profile", "avatar", "user");
        addGroupIfTargetMatches(groups, target,
                "电话", "通话", "拨号", "听筒", "phone", "call", "handset");
        addGroupIfTargetMatches(groups, target,
                "加号", "添加", "新增", "加", "plus", "add");
        addGroupIfTargetMatches(groups, target,
                "wifi", "wi-fi", "无线", "无线网络", "网络", "信号");
        addGroupIfTargetMatches(groups, target,
                "下拉", "展开", "向下", "下箭头", "箭头", "chevron", "down");
        addGroupIfTargetMatches(groups, target,
                "搜索", "放大镜", "查找", "search", "magnifier");
        addGroupIfTargetMatches(groups, target,
                "客服", "耳机", "耳麦", "服务", "headset", "support");
        addGroupIfTargetMatches(groups, target,
                "消息", "聊天", "气泡", "会话", "对话", "chat", "message", "bubble");
        addGroupIfTargetMatches(groups, target,
                "勾选", "勾", "对号", "待办", "稍后", "复选", "check", "tick", "todo");
        addGroupIfTargetMatches(groups, target,
                "星标", "星星", "收藏", "关注", "star", "favorite");
        addGroupIfTargetMatches(groups, target,
                "静音", "铃铛", "免打扰", "通知", "bell", "mute", "silent");
        addGroupIfTargetMatches(groups, target,
                "邮件", "信封", "邮箱", "mail", "email", "envelope");
        addGroupIfTargetMatches(groups, target,
                "通讯录", "联系人", "列表", "名片", "contact", "contacts", "addressbook");
        addGroupIfTargetMatches(groups, target,
                "业务", "九宫格", "网格", "应用", "宫格", "grid", "apps", "workbench");
        addGroupIfTargetMatches(groups, target,
                "知识", "文档", "文件", "书签", "书本", "资料", "doc", "document", "file", "bookmark");
        return groups;
    }

    private static void addGroupIfTargetMatches(List<List<String>> groups, IconTarget target,
                                                String... keywords) {
        List<String> group = Arrays.asList(keywords);
        if (targetTextMatchesGroup(target, group)) {
            groups.add(group);
        }
    }

    private static boolean targetTextMatchesGroup(IconTarget target, List<String> group) {
        if (matchesAnyKeyword(target.expected(), group)) {
            return true;
        }
        if (target.acceptable() != null) {
            for (String alt : target.acceptable()) {
                if (matchesAnyKeyword(alt, group)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesAnyKeyword(String text, List<String> keywords) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            String normalizedKeyword = normalize(keyword);
            if (!normalizedKeyword.isEmpty()
                    && (normalized.contains(normalizedKeyword)
                    || normalizedKeyword.contains(normalized))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("图标", "")
                .replace("icon", "")
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .replace("：", ":")
                .replace("，", ",")
                .replace("。", "")
                .replace(".", "")
                .trim();
    }

    private static String padRight(String text, int width) {
        if (text == null) {
            text = "";
        }
        if (text.length() >= width) {
            return text;
        }
        StringBuilder builder = new StringBuilder(text);
        while (builder.length() < width) {
            builder.append(' ');
        }
        return builder.toString();
    }
}
