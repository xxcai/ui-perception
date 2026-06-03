package com.hh.uiperception.nativeplugin.semantic;

import com.hh.uiperception.core.semantic.*;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class SnapshotRendererTest {

    @Test
    public void rendersSemanticTreeAsSnapshotText() {
        SemanticNode root = SemanticNode.builder(SemanticRole.SCREEN)
                .name("邮件")
                .addChild(SemanticNode.builder(SemanticRole.TOOLBAR)
                        .addChild(SemanticNode.builder(SemanticRole.TEXT)
                                .name("邮件")
                                .bounds(Bounds.parse("[32,48][160,112]"))
                                .build())
                        .addChild(SemanticNode.builder(SemanticRole.BUTTON)
                                .name("搜索")
                                .ref("n1")
                                .bounds(Bounds.parse("[960,40][1040,120]"))
                                .build())
                        .build())
                .addChild(SemanticNode.builder(SemanticRole.LIST)
                        .addState("scrollable")
                        .ref("n2")
                        .bounds(Bounds.parse("[0,160][1080,1900]"))
                        .addChild(SemanticNode.builder(SemanticRole.LIST_ITEM)
                                .addChild(SemanticNode.builder(SemanticRole.TEXT)
                                        .name("吴示例")
                                        .build())
                                .addChild(SemanticNode.builder(SemanticRole.BUTTON)
                                        .name("拨打")
                                        .ref("n3")
                                        .bounds(Bounds.parse("[920,180][1040,260]"))
                                        .build())
                                .build())
                        .build())
                .build();

        String snapshot = SnapshotRenderer.render(root);

        assertEquals(""
                + "- screen \"邮件\":\n"
                + "  - toolbar:\n"
                + "    - text \"邮件\"\n"
                + "    - button \"搜索\" [ref=n1] [bounds=960,40,1040,120]\n"
                + "  - list [scrollable] [ref=n2] [bounds=0,160,1080,1900]:\n"
                + "    - listitem:\n"
                + "      - text \"吴示例\"\n"
                + "      - button \"拨打\" [ref=n3] [bounds=920,180,1040,260]", snapshot);
    }

    @Test
    public void rendersAllBoundsWhenBoxesEnabled() {
        SemanticNode node = SemanticNode.builder(SemanticRole.TEXT)
                .name("邮件")
                .bounds(Bounds.parse("[32,48][160,112]"))
                .build();

        String snapshot = SnapshotRenderer.render(
                node,
                SnapshotRenderOptions.builder().boxes(true).build()
        );

        assertEquals("- text \"邮件\" [bounds=32,48,160,112]", snapshot);
    }

    @Test
    public void limitsDepth() {
        SemanticNode root = SemanticNode.builder(SemanticRole.SCREEN)
                .name("邮件")
                .addChild(SemanticNode.builder(SemanticRole.TOOLBAR)
                        .addChild(SemanticNode.builder(SemanticRole.BUTTON)
                                .name("搜索")
                                .build())
                        .build())
                .build();

        String snapshot = SnapshotRenderer.render(
                root,
                SnapshotRenderOptions.builder().maxDepth(1).build()
        );

        assertEquals("- screen \"邮件\"", snapshot);
    }

    @Test
    public void escapesNames() {
        SemanticNode node = SemanticNode.builder(SemanticRole.BUTTON)
                .name("说\"你好\"")
                .build();

        assertEquals("- button \"说\\\"你好\\\"\"", SnapshotRenderer.render(node));
    }

    @Test
    public void rendersClickableGuessedState() {
        SemanticNode node = SemanticNode.builder(SemanticRole.LIST_ITEM)
                .addState(SemanticStates.CLICKABLE_GUESSED)
                .build();

        assertEquals("- listitem [clickable=guessed]", SnapshotRenderer.render(node));
    }
}
