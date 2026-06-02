package com.hh.uiperception.nativeplugin.evaluation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class NativeEvaluationFixtureTest {

    @Test
    public void loadsMessageFixture() {
        NativeEvaluationFixture fixture = NativeEvaluationFixture.load("message");

        assertEquals("message", fixture.page());
        assertTrue(fixture.rawXml().startsWith("<hierarchy"));
        assertTrue(fixture.snapshot().contains("- scroll [ref=n2]"));
        assertEquals(4, fixture.targets().targets().size());

        EvaluationTarget search = fixture.targets().targets().get(0);
        assertEquals("search-entry", search.id());
        assertEquals("text", search.role());
        assertEquals("搜索", search.name());
        assertFalse(search.requiredRef());

        EvaluationTarget messageList = fixture.targets().targets().get(2);
        assertEquals("message-scroll", messageList.id());
        assertEquals("scroll", messageList.role());
        assertTrue(messageList.requiredRef());
    }

    @Test
    public void rejectsTargetWithoutRequiredRef() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> EvaluationTargets.loadFromString(""
                        + "page: message\n"
                        + "targets:\n"
                        + "  - id: search-entry\n"
                        + "    role: text\n"
                        + "    name: 搜索\n"));

        assertTrue(error.getMessage().contains("requiredRef"));
        assertTrue(error.getMessage().contains("message"));
    }

    @Test
    public void rejectsUnknownRole() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> EvaluationTargets.loadFromString(""
                        + "page: message\n"
                        + "targets:\n"
                        + "  - id: search-entry\n"
                        + "    role: nonexistent_role\n"
                        + "    requiredRef: false\n"));

        assertTrue(error.getMessage().contains("role 非法"));
        assertTrue(error.getMessage().contains("nonexistent_role"));
    }
}
