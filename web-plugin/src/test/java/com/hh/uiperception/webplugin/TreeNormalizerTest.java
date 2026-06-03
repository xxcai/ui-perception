package com.hh.uiperception.webplugin;

import com.hh.uiperception.core.semantic.Bounds;
import com.hh.uiperception.core.semantic.SemanticNode;
import com.hh.uiperception.core.semantic.SemanticRole;

import org.junit.Test;

import static org.junit.Assert.*;

public final class TreeNormalizerTest {

    // --- Generic folding ---

    @Test
    public void foldsAnonymousSingleChildGeneric() {
        SemanticNode inner = SemanticNode.builder(SemanticRole.BUTTON)
                .name("Click").bounds(new Bounds(0, 0, 100, 50))
                .build();
        SemanticNode wrapper = SemanticNode.builder(SemanticRole.GENERIC)
                .name("").bounds(new Bounds(0, 0, 100, 50))
                .addChild(inner)
                .build();

        SemanticNode result = TreeNormalizer.normalize(wrapper);
        assertEquals(SemanticRole.BUTTON, result.role());
        assertEquals("Click", result.name());
    }

    @Test
    public void foldsAnonymousZeroChildGeneric() {
        SemanticNode empty = SemanticNode.builder(SemanticRole.GENERIC)
                .name("").bounds(new Bounds(0, 0, 100, 50))
                .build();

        SemanticNode result = TreeNormalizer.normalize(empty);
        assertEquals(SemanticRole.GENERIC, result.role());
    }

    @Test
    public void doesNotFoldGenericWithRef() {
        SemanticNode inner = SemanticNode.builder(SemanticRole.TEXT)
                .name("hello").bounds(new Bounds(0, 0, 100, 20))
                .build();
        SemanticNode wrapper = SemanticNode.builder(SemanticRole.GENERIC)
                .name("").ref("w1").bounds(new Bounds(0, 0, 100, 50))
                .addChild(inner)
                .build();

        SemanticNode result = TreeNormalizer.normalize(wrapper);
        assertEquals(SemanticRole.GENERIC, result.role());
        assertEquals("w1", result.ref());
    }

    @Test
    public void doesNotFoldGenericWithName() {
        SemanticNode inner = SemanticNode.builder(SemanticRole.TEXT)
                .name("hello").bounds(new Bounds(0, 0, 100, 20))
                .build();
        SemanticNode wrapper = SemanticNode.builder(SemanticRole.GENERIC)
                .name("container").bounds(new Bounds(0, 0, 100, 50))
                .addChild(inner)
                .build();

        SemanticNode result = TreeNormalizer.normalize(wrapper);
        assertEquals(SemanticRole.GENERIC, result.role());
        assertEquals("container", result.name());
    }

    @Test
    public void mergesTwoTextsThenFoldsGeneric() {
        // Two text children merge into one, then anonymous generic with one child folds
        SemanticNode child1 = SemanticNode.builder(SemanticRole.TEXT)
                .name("a").bounds(new Bounds(0, 0, 50, 20))
                .build();
        SemanticNode child2 = SemanticNode.builder(SemanticRole.TEXT)
                .name("b").bounds(new Bounds(50, 0, 100, 20))
                .build();
        SemanticNode wrapper = SemanticNode.builder(SemanticRole.GENERIC)
                .name("").bounds(new Bounds(0, 0, 100, 50))
                .addChild(child1).addChild(child2)
                .build();

        SemanticNode result = TreeNormalizer.normalize(wrapper);
        // merged text "a b" then generic folded away
        assertEquals(SemanticRole.TEXT, result.role());
        assertEquals("a b", result.name());
    }

    @Test
    public void doesNotFoldGenericWithMultipleNonTextChildren() {
        SemanticNode child1 = SemanticNode.builder(SemanticRole.BUTTON)
                .name("A").bounds(new Bounds(0, 0, 50, 30))
                .build();
        SemanticNode child2 = SemanticNode.builder(SemanticRole.BUTTON)
                .name("B").bounds(new Bounds(50, 0, 100, 30))
                .build();
        SemanticNode wrapper = SemanticNode.builder(SemanticRole.GENERIC)
                .name("").bounds(new Bounds(0, 0, 100, 50))
                .addChild(child1).addChild(child2)
                .build();

        SemanticNode result = TreeNormalizer.normalize(wrapper);
        assertEquals(SemanticRole.GENERIC, result.role());
        assertEquals(2, result.children().size());
    }

    // --- Consecutive text merging ---

    @Test
    public void mergesConsecutiveTextChildren() {
        SemanticNode t1 = SemanticNode.builder(SemanticRole.TEXT).name("Hello").build();
        SemanticNode t2 = SemanticNode.builder(SemanticRole.TEXT).name("World").build();
        // Use a named generic so it doesn't fold
        SemanticNode parent = SemanticNode.builder(SemanticRole.GENERIC)
                .name("container").bounds(new Bounds(0, 0, 100, 50))
                .addChild(t1).addChild(t2)
                .build();

        SemanticNode result = TreeNormalizer.normalize(parent);
        assertEquals(1, result.children().size());
        assertEquals("Hello World", result.children().get(0).name());
    }

    @Test
    public void doesNotMergeTextSeparatedByNonText() {
        SemanticNode t1 = SemanticNode.builder(SemanticRole.TEXT).name("Hello").build();
        SemanticNode btn = SemanticNode.builder(SemanticRole.BUTTON).name("Click").bounds(new Bounds(0, 0, 50, 30)).build();
        SemanticNode t2 = SemanticNode.builder(SemanticRole.TEXT).name("World").build();
        SemanticNode parent = SemanticNode.builder(SemanticRole.GENERIC)
                .name("container").bounds(new Bounds(0, 0, 100, 50))
                .addChild(t1).addChild(btn).addChild(t2)
                .build();

        SemanticNode result = TreeNormalizer.normalize(parent);
        assertEquals(3, result.children().size());
    }

    // --- Self-name dedup ---

    @Test
    public void removesTextChildDuplicateOfName() {
        SemanticNode textChild = SemanticNode.builder(SemanticRole.TEXT).name("Submit").build();
        SemanticNode parent = SemanticNode.builder(SemanticRole.BUTTON)
                .name("Submit").bounds(new Bounds(0, 0, 100, 50))
                .addChild(textChild)
                .build();

        SemanticNode result = TreeNormalizer.normalize(parent);
        assertEquals(0, result.children().size());
    }

    @Test
    public void keepsTextChildDifferentFromName() {
        SemanticNode textChild = SemanticNode.builder(SemanticRole.TEXT).name("Submit form").build();
        SemanticNode parent = SemanticNode.builder(SemanticRole.BUTTON)
                .name("Submit").bounds(new Bounds(0, 0, 100, 50))
                .addChild(textChild)
                .build();

        SemanticNode result = TreeNormalizer.normalize(parent);
        assertEquals(1, result.children().size());
    }

    // --- Null ---

    @Test
    public void returnsNullForNull() {
        assertNull(TreeNormalizer.normalize(null));
    }
}
