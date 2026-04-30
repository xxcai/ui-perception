package com.hh.uiperception.nativeplugin.semantic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class NativeSnapshotRendererTest {

    @Test
    public void rendersSemanticTreeAsSnapshotText() {
        NativeSemanticNode root = NativeSemanticNode.builder(NativeSemanticRole.SCREEN)
                .name("邮件")
                .addChild(NativeSemanticNode.builder(NativeSemanticRole.TOOLBAR)
                        .addChild(NativeSemanticNode.builder(NativeSemanticRole.TEXT)
                                .name("邮件")
                                .bounds(NativeBounds.parse("[32,48][160,112]"))
                                .build())
                        .addChild(NativeSemanticNode.builder(NativeSemanticRole.BUTTON)
                                .name("搜索")
                                .ref("n1")
                                .bounds(NativeBounds.parse("[960,40][1040,120]"))
                                .build())
                        .build())
                .addChild(NativeSemanticNode.builder(NativeSemanticRole.LIST)
                        .addState("scrollable")
                        .ref("n2")
                        .bounds(NativeBounds.parse("[0,160][1080,1900]"))
                        .addChild(NativeSemanticNode.builder(NativeSemanticRole.LIST_ITEM)
                                .addChild(NativeSemanticNode.builder(NativeSemanticRole.TEXT)
                                        .name("张三")
                                        .build())
                                .addChild(NativeSemanticNode.builder(NativeSemanticRole.BUTTON)
                                        .name("拨打")
                                        .ref("n3")
                                        .bounds(NativeBounds.parse("[920,180][1040,260]"))
                                        .build())
                                .build())
                        .build())
                .build();

        String snapshot = NativeSnapshotRenderer.render(root);

        assertEquals(""
                + "- screen \"邮件\":\n"
                + "  - toolbar:\n"
                + "    - text \"邮件\"\n"
                + "    - button \"搜索\" [ref=n1] [bounds=960,40,1040,120]\n"
                + "  - list [scrollable] [ref=n2] [bounds=0,160,1080,1900]:\n"
                + "    - listitem:\n"
                + "      - text \"张三\"\n"
                + "      - button \"拨打\" [ref=n3] [bounds=920,180,1040,260]", snapshot);
    }

    @Test
    public void rendersAllBoundsWhenBoxesEnabled() {
        NativeSemanticNode node = NativeSemanticNode.builder(NativeSemanticRole.TEXT)
                .name("邮件")
                .bounds(NativeBounds.parse("[32,48][160,112]"))
                .build();

        String snapshot = NativeSnapshotRenderer.render(
                node,
                NativeSnapshotRenderOptions.builder().boxes(true).build()
        );

        assertEquals("- text \"邮件\" [bounds=32,48,160,112]", snapshot);
    }

    @Test
    public void limitsDepth() {
        NativeSemanticNode root = NativeSemanticNode.builder(NativeSemanticRole.SCREEN)
                .name("邮件")
                .addChild(NativeSemanticNode.builder(NativeSemanticRole.TOOLBAR)
                        .addChild(NativeSemanticNode.builder(NativeSemanticRole.BUTTON)
                                .name("搜索")
                                .build())
                        .build())
                .build();

        String snapshot = NativeSnapshotRenderer.render(
                root,
                NativeSnapshotRenderOptions.builder().maxDepth(1).build()
        );

        assertEquals("- screen \"邮件\"", snapshot);
    }

    @Test
    public void escapesNames() {
        NativeSemanticNode node = NativeSemanticNode.builder(NativeSemanticRole.BUTTON)
                .name("说\"你好\"")
                .build();

        assertEquals("- button \"说\\\"你好\\\"\"", NativeSnapshotRenderer.render(node));
    }
}
