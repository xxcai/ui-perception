package com.hh.uiperception.nativeplugin.semantic;

import com.hh.uiperception.core.semantic.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class RefAssignerTest {

    @Test
    public void returnsNullForNullRoot() {
        assertNull(RefAssigner.assign(null));
    }

    @Test
    public void assignsRefsToExecutableNodesWithValidBoundsInTraversalOrder() {
        SemanticNode root = SemanticNode.builder(SemanticRole.SCREEN)
                .name("邮件")
                .addChild(SemanticNode.builder(SemanticRole.TEXT)
                        .name("邮件")
                        .bounds(Bounds.parse("[32,48][160,112]"))
                        .build())
                .addChild(SemanticNode.builder(SemanticRole.BUTTON)
                        .name("搜索")
                        .bounds(Bounds.parse("[960,40][1040,120]"))
                        .build())
                .addChild(SemanticNode.builder(SemanticRole.LIST)
                        .addState("scrollable")
                        .bounds(Bounds.parse("[0,160][1080,1900]"))
                        .addChild(SemanticNode.builder(SemanticRole.BUTTON)
                                .name("拨打")
                                .bounds(Bounds.parse("[920,180][1040,260]"))
                                .build())
                        .build())
                .build();

        SemanticNode assigned = RefAssigner.assign(root);

        assertFalse(assigned.hasRef());
        assertFalse(assigned.children().get(0).hasRef());
        assertEquals("n1", assigned.children().get(1).ref());
        assertEquals("n2", assigned.children().get(2).ref());
        assertEquals("n3", assigned.children().get(2).children().get(0).ref());
    }

    @Test
    public void skipsExecutableNodesWithoutValidBounds() {
        SemanticNode root = SemanticNode.builder(SemanticRole.BUTTON)
                .name("搜索")
                .bounds(Bounds.parse("[10,10][10,20]"))
                .build();

        SemanticNode assigned = RefAssigner.assign(root);

        assertFalse(assigned.hasRef());
    }

    @Test
    public void keepsExistingRefWhenNodeIsNotAssignable() {
        SemanticNode root = SemanticNode.builder(SemanticRole.TEXT)
                .name("审批通过")
                .ref("status")
                .build();

        SemanticNode assigned = RefAssigner.assign(root);

        assertEquals("status", assigned.ref());
    }

    @Test
    public void assignsRefsToListItemsWithoutExecutableDescendants() {
        SemanticNode root = SemanticNode.builder(SemanticRole.LIST)
                .bounds(Bounds.parse("[0,100][1080,900]"))
                .addChild(SemanticNode.builder(SemanticRole.LIST_ITEM)
                        .addState(SemanticStates.CLICKABLE_GUESSED)
                        .bounds(Bounds.parse("[0,100][1080,260]"))
                        .addChild(SemanticNode.builder(SemanticRole.TEXT)
                                .name("标题")
                                .bounds(Bounds.parse("[42,120][400,180]"))
                                .build())
                        .build())
                .build();

        SemanticNode assigned = RefAssigner.assign(root);

        assertEquals("n1", assigned.ref());
        assertEquals("n2", assigned.children().get(0).ref());
        assertFalse(assigned.children().get(0).children().get(0).hasRef());
    }

    @Test
    public void skipsListItemRefWhenItWrapsExecutableDescendant() {
        SemanticNode root = SemanticNode.builder(SemanticRole.LIST)
                .bounds(Bounds.parse("[0,100][1080,900]"))
                .addChild(SemanticNode.builder(SemanticRole.LIST_ITEM)
                        .addState(SemanticStates.CLICKABLE_GUESSED)
                        .bounds(Bounds.parse("[0,100][1080,260]"))
                        .addChild(SemanticNode.builder(SemanticRole.GENERIC)
                                .addChild(SemanticNode.builder(SemanticRole.BUTTON)
                                        .name("搜索")
                                        .bounds(Bounds.parse("[42,120][400,180]"))
                                        .build())
                                .build())
                        .build())
                .build();

        SemanticNode assigned = RefAssigner.assign(root);
        SemanticNode item = assigned.children().get(0);

        assertFalse(item.hasRef());
        assertEquals("n2", item.children().get(0).children().get(0).ref());
    }

    @Test
    public void assignsRefToClickableListItemWithExecutableDescendant() {
        SemanticNode root = SemanticNode.builder(SemanticRole.LIST)
                .bounds(Bounds.parse("[0,100][1080,900]"))
                .addChild(SemanticNode.builder(SemanticRole.LIST_ITEM)
                        .addState(SemanticStates.CLICKABLE)
                        .bounds(Bounds.parse("[0,100][1080,260]"))
                        .addChild(SemanticNode.builder(SemanticRole.BUTTON)
                                .name("购买")
                                .bounds(Bounds.parse("[920,180][1040,260]"))
                                .build())
                        .build())
                .build();

        SemanticNode assigned = RefAssigner.assign(root);
        SemanticNode item = assigned.children().get(0);

        assertEquals("n2", item.ref());
        assertEquals("n3", item.children().get(0).ref());
    }

    @Test
    public void assignsRefToClickableInferredListItemWithExecutableDescendant() {
        SemanticNode root = SemanticNode.builder(SemanticRole.LIST)
                .bounds(Bounds.parse("[0,100][1080,900]"))
                .addChild(SemanticNode.builder(SemanticRole.LIST_ITEM)
                        .addState(SemanticStates.CLICKABLE_INFERRED)
                        .bounds(Bounds.parse("[0,100][1080,260]"))
                        .addChild(SemanticNode.builder(SemanticRole.BUTTON)
                                .name("拨打")
                                .bounds(Bounds.parse("[920,180][1040,260]"))
                                .build())
                        .build())
                .build();

        SemanticNode assigned = RefAssigner.assign(root);
        SemanticNode item = assigned.children().get(0);

        assertEquals("n2", item.ref());
        assertEquals("n3", item.children().get(0).ref());
    }

    @Test
    public void skipsListItemWithoutClickableState() {
        SemanticNode root = SemanticNode.builder(SemanticRole.LIST)
                .bounds(Bounds.parse("[0,100][1080,900]"))
                .addChild(SemanticNode.builder(SemanticRole.LIST_ITEM)
                        .bounds(Bounds.parse("[0,100][1080,260]"))
                        .build())
                .build();

        SemanticNode assigned = RefAssigner.assign(root);

        assertEquals("n1", assigned.ref());
        assertFalse(assigned.children().get(0).hasRef());
    }
}
