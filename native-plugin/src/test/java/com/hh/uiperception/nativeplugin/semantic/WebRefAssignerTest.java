// Quick test to verify web mode ref assignment
package com.hh.uiperception.nativeplugin.semantic;

import com.hh.uiperception.core.semantic.*;

import static org.junit.Assert.*;

import org.junit.Test;

public class WebRefAssignerTest {

    @Test
    public void webModeAssignsRefToAllVisibleNodes() {
        SemanticNode root = SemanticNode.builder(SemanticRole.SCREEN)
                .name("Web Page")
                .bounds(Bounds.parse("[0,0][1080,2400]"))
                .addChild(SemanticNode.builder(SemanticRole.HEADING)
                        .name("Title")
                        .bounds(Bounds.parse("[16,100][400,150]"))
                        .build())
                .addChild(SemanticNode.builder(SemanticRole.LINK)
                        .name("a link")
                        .addState("clickable")
                        .bounds(Bounds.parse("[100,200][200,220]"))
                        .build())
                .addChild(SemanticNode.builder(SemanticRole.GENERIC)
                        .name("paragraph text")
                        .bounds(Bounds.parse("[16,250][400,300]"))
                        .build())
                .build();

        SemanticNode assigned = RefAssigner.assign(root, "w", true);

        // web mode: all nodes with valid bounds get refs
        assertEquals("w1", assigned.ref());          // screen
        assertEquals("w2", assigned.children().get(0).ref()); // heading
        assertEquals("w3", assigned.children().get(1).ref()); // link
        assertEquals("w4", assigned.children().get(2).ref()); // generic
    }

    @Test
    public void webModeSkipsNodesWithoutValidBounds() {
        SemanticNode root = SemanticNode.builder(SemanticRole.SCREEN)
                .name("Page")
                .bounds(Bounds.parse("[0,0][1080,2400]"))
                .addChild(SemanticNode.builder(SemanticRole.TEXT)
                        .name("no bounds text")
                        .build())
                .addChild(SemanticNode.builder(SemanticRole.BUTTON)
                        .name("button")
                        .bounds(Bounds.parse("[10,10][100,50]"))
                        .build())
                .build();

        SemanticNode assigned = RefAssigner.assign(root, "w", true);

        assertEquals("w1", assigned.ref());
        assertFalse(assigned.children().get(0).hasRef()); // no bounds
        assertEquals("w2", assigned.children().get(1).ref()); // has bounds
    }

    @Test
    public void nativeModeDoesNotAssignRefToGenericOrLink() {
        SemanticNode root = SemanticNode.builder(SemanticRole.SCREEN)
                .name("Page")
                .addChild(SemanticNode.builder(SemanticRole.GENERIC)
                        .name("container")
                        .bounds(Bounds.parse("[0,100][1080,500]"))
                        .build())
                .addChild(SemanticNode.builder(SemanticRole.LINK)
                        .name("a link")
                        .addState("clickable")
                        .bounds(Bounds.parse("[100,200][200,220]"))
                        .build())
                .addChild(SemanticNode.builder(SemanticRole.BUTTON)
                        .name("button")
                        .bounds(Bounds.parse("[10,300][100,350]"))
                        .build())
                .build();

        SemanticNode assigned = RefAssigner.assign(root, "n", false);

        assertFalse(assigned.hasRef());                              // screen
        assertFalse(assigned.children().get(0).hasRef());           // generic
        assertFalse(assigned.children().get(1).hasRef());           // link (not in whitelist)
        assertEquals("n1", assigned.children().get(2).ref());       // button (whitelist)
    }
}
