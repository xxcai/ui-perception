package com.hh.uiperception.nativeplugin.evaluation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 路线二评测样本：raw XML、semantic snapshot 与 targets 标注。
 */
public final class NativeEvaluationFixture {

    private final String page;
    private final String rawXml;
    private final String snapshot;
    private final EvaluationTargets targets;

    private NativeEvaluationFixture(
            String page,
            String rawXml,
            String snapshot,
            EvaluationTargets targets) {
        this.page = page;
        this.rawXml = rawXml;
        this.snapshot = snapshot;
        this.targets = targets;
    }

    public String page() {
        return page;
    }

    public String rawXml() {
        return rawXml;
    }

    public String snapshot() {
        return snapshot;
    }

    public EvaluationTargets targets() {
        return targets;
    }

    public static NativeEvaluationFixture load(String page) {
        String base = "native-evaluation/" + page + "/";
        String rawXml = readRequiredResource(base + "raw.xml", page);
        String snapshot = readRequiredResource(base + "snapshot.yml", page);
        EvaluationTargets targets = EvaluationTargets.load(
                openRequiredResource(base + "targets.yml", page), page);
        if (!page.equals(targets.page())) {
            throw new IllegalArgumentException(
                    "targets page 与目录名不一致: " + targets.page() + " != " + page);
        }
        return new NativeEvaluationFixture(page, rawXml, snapshot, targets);
    }

    private static String readRequiredResource(String path, String page) {
        try (InputStream in = openRequiredResource(path, page)) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) {
                throw new IllegalArgumentException("评测资源为空: " + path + " (页面: " + page + ")");
            }
            return content;
        } catch (IOException e) {
            throw new RuntimeException("读取评测资源失败: " + path, e);
        }
    }

    private static InputStream openRequiredResource(String path, String page) {
        InputStream in = NativeEvaluationFixture.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalArgumentException("缺少评测资源: " + path + " (页面: " + page + ")");
        }
        return in;
    }
}
