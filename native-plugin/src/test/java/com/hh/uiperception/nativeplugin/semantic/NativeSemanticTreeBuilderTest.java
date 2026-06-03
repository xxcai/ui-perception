package com.hh.uiperception.nativeplugin.semantic;

import com.hh.uiperception.core.semantic.*;

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
                .bounds(Bounds.parse("[0,160][1080,1900]"))
                .addChild(NativeViewNode.builder()
                        .className("android.widget.TextView")
                        .text("提交")
                        .clickable(true)
                        .enabled(false)
                        .bounds(Bounds.parse("[800,100][1000,180]"))
                        .build())
                .build();

        SemanticNode semantic = NativeSemanticTreeBuilder.build(root);

        assertNotNull(semantic);
        assertEquals(SemanticRole.LIST, semantic.role());
        assertTrue(semantic.states().contains("scrollable"));
        assertEquals("0,160,1080,1900", semantic.bounds().toSnapshotValue());
        assertEquals(1, semantic.children().size());

        SemanticNode item = semantic.children().get(0);
        assertEquals(SemanticRole.LIST_ITEM, item.role());
        assertEquals("提交", item.name());
        assertTrue(item.states().contains("disabled"));
        assertTrue(item.states().contains(SemanticStates.CLICKABLE));
        assertEquals("structure:collection-item-clickable", item.roleDecision().source());
    }

    @Test
    public void foldsSingleChildGenericWrapper() {
        NativeViewNode root = NativeViewNode.builder()
                .className("android.widget.LinearLayout")
                .addChild(NativeViewNode.builder()
                        .className("android.widget.TextView")
                        .text("吴示例")
                        .build())
                .build();

        SemanticNode semantic = NativeSemanticTreeBuilder.build(root);

        assertNotNull(semantic);
        assertEquals(SemanticRole.TEXT, semantic.role());
        assertEquals("吴示例", semantic.name());
    }

    @Test
    public void keepsGenericWrapperWithMultipleSemanticChildren() {
        NativeViewNode root = NativeViewNode.builder()
                .className("android.widget.LinearLayout")
                .addChild(NativeViewNode.builder()
                        .className("android.widget.TextView")
                        .text("吴示例")
                        .build())
                .addChild(NativeViewNode.builder()
                        .className("android.widget.TextView")
                        .text("郑样例")
                        .build())
                .build();

        SemanticNode semantic = NativeSemanticTreeBuilder.build(root);

        assertNotNull(semantic);
        assertEquals(SemanticRole.GENERIC, semantic.role());
        assertEquals(2, semantic.children().size());
    }

    @Test
    public void treatsDirectCollectionContainerChildrenAsListItems() {
        NativeViewNode root = NativeViewNode.builder()
                .className("androidx.recyclerview.widget.RecyclerView")
                .bounds(Bounds.parse("[0,100][1080,900]"))
                .addChild(NativeViewNode.builder()
                        .className("android.widget.LinearLayout")
                        .bounds(Bounds.parse("[0,100][1080,260]"))
                        .addChild(NativeViewNode.builder()
                                .className("android.widget.TextView")
                                .text("标题")
                                .bounds(Bounds.parse("[42,120][400,180]"))
                                .build())
                        .build())
                .build();

        SemanticNode semantic = NativeSemanticTreeBuilder.build(root);

        assertNotNull(semantic);
        assertEquals(SemanticRole.LIST, semantic.role());
        assertEquals(1, semantic.children().size());
        assertEquals(SemanticRole.LIST_ITEM, semantic.children().get(0).role());
        assertEquals("structure:collection-item", semantic.children().get(0).roleDecision().source());
    }

    @Test
    public void clickableCollectionChildKeepsListItemRole() {
        NativeViewNode root = NativeViewNode.builder()
                .className("android.widget.ListView")
                .bounds(Bounds.parse("[0,100][1080,900]"))
                .addChild(NativeViewNode.builder()
                        .className("android.widget.LinearLayout")
                        .clickable(true)
                        .bounds(Bounds.parse("[0,100][1080,260]"))
                        .addChild(NativeViewNode.builder()
                                .className("android.widget.TextView")
                                .text("标题")
                                .build())
                        .build())
                .build();

        SemanticNode semantic = NativeSemanticTreeBuilder.build(root);

        assertNotNull(semantic);
        assertEquals(SemanticRole.LIST_ITEM, semantic.children().get(0).role());
        assertEquals("structure:collection-item-clickable", semantic.children().get(0).roleDecision().source());
        assertTrue(semantic.children().get(0).states().contains(SemanticStates.CLICKABLE));
    }

    @Test
    public void nonCollectionClickableGenericRemainsButton() {
        NativeViewNode root = NativeViewNode.builder()
                .className("android.widget.LinearLayout")
                .clickable(true)
                .bounds(Bounds.parse("[0,100][1080,260]"))
                .addChild(NativeViewNode.builder()
                        .className("android.widget.TextView")
                        .text("标题")
                        .build())
                .build();

        SemanticNode semantic = NativeSemanticTreeBuilder.build(root);

        assertNotNull(semantic);
        assertEquals(SemanticRole.BUTTON, semantic.role());
        assertEquals("attribute:clickable", semantic.roleDecision().source());
    }

    @Test
    public void listItemWithParentItemClickListenerGetsClickableInferredState() {
        NativeViewNode root = NativeViewNode.builder()
                .className("android.widget.ListView")
                .hasItemClickListener(true)
                .bounds(Bounds.parse("[0,100][1080,900]"))
                .addChild(NativeViewNode.builder()
                        .className("android.widget.LinearLayout")
                        .bounds(Bounds.parse("[0,100][1080,260]"))
                        .addChild(NativeViewNode.builder()
                                .className("android.widget.TextView")
                                .text("标题")
                                .build())
                        .build())
                .build();

        SemanticNode semantic = NativeSemanticTreeBuilder.build(root);

        assertNotNull(semantic);
        SemanticNode item = semantic.children().get(0);
        assertEquals(SemanticRole.LIST_ITEM, item.role());
        assertTrue(item.states().contains(SemanticStates.CLICKABLE_INFERRED));
    }

    @Test
    public void listItemWithoutClickSignalsGetsClickableGuessedState() {
        NativeViewNode root = NativeViewNode.builder()
                .className("androidx.recyclerview.widget.RecyclerView")
                .bounds(Bounds.parse("[0,100][1080,900]"))
                .addChild(NativeViewNode.builder()
                        .className("android.widget.LinearLayout")
                        .bounds(Bounds.parse("[0,100][1080,260]"))
                        .build())
                .build();

        SemanticNode semantic = NativeSemanticTreeBuilder.build(root);

        assertNotNull(semantic);
        SemanticNode item = semantic.children().get(0);
        assertEquals(SemanticRole.LIST_ITEM, item.role());
        assertTrue(item.states().contains(SemanticStates.CLICKABLE_GUESSED));
    }

    @Test
    public void marksUnnamedImageButtonAsNeedingVisualDescription() {
        NativeViewNode root = NativeViewNode.builder()
                .className("android.widget.ImageButton")
                .clickable(true)
                .bounds(Bounds.parse("[900,120][1000,220]"))
                .build();

        SemanticNode semantic = NativeSemanticTreeBuilder.build(root);

        assertNotNull(semantic);
        assertEquals(SemanticRole.BUTTON, semantic.role());
        assertTrue(semantic.states().contains("needs_visual_desc"));
    }

    @Test
    public void doesNotMarkNamedImageButtonAsNeedingVisualDescription() {
        NativeViewNode root = NativeViewNode.builder()
                .className("android.widget.ImageButton")
                .contentDescription("搜索")
                .clickable(true)
                .bounds(Bounds.parse("[900,120][1000,220]"))
                .build();

        SemanticNode semantic = NativeSemanticTreeBuilder.build(root);

        assertNotNull(semantic);
        assertEquals(SemanticRole.BUTTON, semantic.role());
        assertEquals("搜索", semantic.name());
        assertTrue(!semantic.states().contains("needs_visual_desc"));
    }
}
