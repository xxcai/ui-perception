package com.hh.uiperception.smallmodelplugin.experiment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public final class IconResultMatcherTest {

    @Test
    public void matchesSemanticSynonyms() {
        IconTarget target = new IconTarget(
                "t012",
                new IconBounds(0, 0, 10, 10),
                "邮件图标",
                Arrays.asList("邮件", "信封", "邮箱")
        );
        List<ParsedIconDescription> output = Collections.singletonList(
                new ParsedIconDescription("t012", "一个信封形状")
        );

        List<IconMatchResult> results = IconResultMatcher.match(output,
                Collections.singletonList(target));

        assertTrue(results.get(0).matched());
    }

    @Test
    public void matchesEnglishVisualSynonyms() {
        IconTarget target = new IconTarget(
                "t014",
                new IconBounds(0, 0, 10, 10),
                "业务九宫格图标",
                Arrays.asList("业务", "九宫格", "网格", "应用")
        );
        List<ParsedIconDescription> output = Collections.singletonList(
                new ParsedIconDescription("t014", "apps grid")
        );

        List<IconMatchResult> results = IconResultMatcher.match(output,
                Collections.singletonList(target));

        assertTrue(results.get(0).matched());
    }

    @Test
    public void rejectsDifferentIconSemantics() {
        IconTarget target = new IconTarget(
                "t015",
                new IconBounds(0, 0, 10, 10),
                "知识文档图标",
                Arrays.asList("知识", "文档", "书签", "文件")
        );
        List<ParsedIconDescription> output = Collections.singletonList(
                new ParsedIconDescription("t015", "电话听筒")
        );

        List<IconMatchResult> results = IconResultMatcher.match(output,
                Collections.singletonList(target));

        assertFalse(results.get(0).matched());
    }
}
