package com.hh.uiperception.smallmodelplugin.experiment;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public final class IconOutputParserTest {

    @Test
    public void parsesColonSeparatedDescriptions() {
        List<ParsedIconDescription> parsed = IconOutputParser.parse(""
                + "icon_001:电话图标\n"
                + "icon_002：加号图标\n");

        assertEquals(2, parsed.size());
        assertEquals("icon_001", parsed.get(0).id());
        assertEquals("电话图标", parsed.get(0).desc());
        assertEquals("icon_002", parsed.get(1).id());
        assertEquals("加号图标", parsed.get(1).desc());
    }

    @Test
    public void ignoresInvalidLines() {
        List<ParsedIconDescription> parsed = IconOutputParser.parse(""
                + "extra text\n"
                + "icon_001:\n"
                + ":电话\n"
                + "icon_002:unknown\n");

        assertEquals(1, parsed.size());
        assertEquals("icon_002", parsed.get(0).id());
        assertEquals("unknown", parsed.get(0).desc());
    }

    @Test
    public void stripsMarkdownFence() {
        List<ParsedIconDescription> parsed = IconOutputParser.parse("```text\n"
                + "icon_001:电话图标\n"
                + "```");

        assertEquals(1, parsed.size());
        assertEquals("icon_001", parsed.get(0).id());
        assertEquals("电话图标", parsed.get(0).desc());
    }

    @Test
    public void remapsNumericLabelsBackToTargetIds() {
        List<ParsedIconDescription> parsed = IconOutputParser.parse(""
                        + "1:电话\n"
                        + "2:加号\n",
                java.util.Arrays.asList(
                        new IconTargetMapping("t001", null, null, "1"),
                        new IconTargetMapping("t002", null, null, "2")
                ));

        assertEquals(2, parsed.size());
        assertEquals("t001", parsed.get(0).id());
        assertEquals("电话", parsed.get(0).desc());
        assertEquals("t002", parsed.get(1).id());
        assertEquals("加号", parsed.get(1).desc());
    }
}
