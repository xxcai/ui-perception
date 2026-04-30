package com.hh.uiperception.nativeplugin.semantic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class NativeSemanticTreeBuilderTest {

    @Test
    public void returnsNullForNullRoot() {
        assertNull(NativeSemanticTreeBuilder.build(null));
    }

    @Test
    public void resolvesRoleNameStateAndBounds() {
        NativeViewNode root = NativeViewNode.builder()
                .className("androidx.recyclerview.widget.RecyclerView")
                .scrollable(true)
                .bounds(NativeBounds.parse("[0,160][1080,1900]"))
                .addChild(NativeViewNode.builder()
                        .className("android.widget.TextView")
                        .text("提交")
                        .clickable(true)
                        .enabled(false)
                        .bounds(NativeBounds.parse("[800,100][1000,180]"))
                        .build())
                .build();

        NativeSemanticNode semantic = NativeSemanticTreeBuilder.build(root);

        assertNotNull(semantic);
        assertEquals(NativeSemanticRole.LIST, semantic.role());
        assertTrue(semantic.states().contains("scrollable"));
        assertEquals("0,160,1080,1900", semantic.bounds().toSnapshotValue());
        assertEquals(1, semantic.children().size());

        NativeSemanticNode button = semantic.children().get(0);
        assertEquals(NativeSemanticRole.BUTTON, button.role());
        assertEquals("提交", button.name());
        assertTrue(button.states().contains("disabled"));
        assertEquals("attribute:clickable", button.roleDecision().source());
    }

    @Test
    public void foldsSingleChildGenericWrapper() {
        NativeViewNode root = NativeViewNode.builder()
                .className("android.widget.LinearLayout")
                .addChild(NativeViewNode.builder()
                        .className("android.widget.TextView")
                        .text("张三")
                        .build())
                .build();

        NativeSemanticNode semantic = NativeSemanticTreeBuilder.build(root);

        assertNotNull(semantic);
        assertEquals(NativeSemanticRole.TEXT, semantic.role());
        assertEquals("张三", semantic.name());
    }

    @Test
    public void keepsGenericWrapperWithMultipleSemanticChildren() {
        NativeViewNode root = NativeViewNode.builder()
                .className("android.widget.LinearLayout")
                .addChild(NativeViewNode.builder()
                        .className("android.widget.TextView")
                        .text("张三")
                        .build())
                .addChild(NativeViewNode.builder()
                        .className("android.widget.TextView")
                        .text("李四")
                        .build())
                .build();

        NativeSemanticNode semantic = NativeSemanticTreeBuilder.build(root);

        assertNotNull(semantic);
        assertEquals(NativeSemanticRole.GENERIC, semantic.role());
        assertEquals(2, semantic.children().size());
    }
}
