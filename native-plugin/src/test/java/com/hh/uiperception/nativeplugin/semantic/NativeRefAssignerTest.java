package com.hh.uiperception.nativeplugin.semantic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class NativeRefAssignerTest {

    @Test
    public void returnsNullForNullRoot() {
        assertNull(NativeRefAssigner.assign(null));
    }

    @Test
    public void assignsRefsToExecutableNodesWithValidBoundsInTraversalOrder() {
        NativeSemanticNode root = NativeSemanticNode.builder(NativeSemanticRole.SCREEN)
                .name("邮件")
                .addChild(NativeSemanticNode.builder(NativeSemanticRole.TEXT)
                        .name("邮件")
                        .bounds(NativeBounds.parse("[32,48][160,112]"))
                        .build())
                .addChild(NativeSemanticNode.builder(NativeSemanticRole.BUTTON)
                        .name("搜索")
                        .bounds(NativeBounds.parse("[960,40][1040,120]"))
                        .build())
                .addChild(NativeSemanticNode.builder(NativeSemanticRole.LIST)
                        .addState("scrollable")
                        .bounds(NativeBounds.parse("[0,160][1080,1900]"))
                        .addChild(NativeSemanticNode.builder(NativeSemanticRole.BUTTON)
                                .name("拨打")
                                .bounds(NativeBounds.parse("[920,180][1040,260]"))
                                .build())
                        .build())
                .build();

        NativeSemanticNode assigned = NativeRefAssigner.assign(root);

        assertFalse(assigned.hasRef());
        assertFalse(assigned.children().get(0).hasRef());
        assertEquals("n1", assigned.children().get(1).ref());
        assertEquals("n2", assigned.children().get(2).ref());
        assertEquals("n3", assigned.children().get(2).children().get(0).ref());
    }

    @Test
    public void skipsExecutableNodesWithoutValidBounds() {
        NativeSemanticNode root = NativeSemanticNode.builder(NativeSemanticRole.BUTTON)
                .name("搜索")
                .bounds(NativeBounds.parse("[10,10][10,20]"))
                .build();

        NativeSemanticNode assigned = NativeRefAssigner.assign(root);

        assertFalse(assigned.hasRef());
    }

    @Test
    public void keepsExistingRefWhenNodeIsNotAssignable() {
        NativeSemanticNode root = NativeSemanticNode.builder(NativeSemanticRole.TEXT)
                .name("审批通过")
                .ref("status")
                .build();

        NativeSemanticNode assigned = NativeRefAssigner.assign(root);

        assertEquals("status", assigned.ref());
    }
}
