package com.hh.uiperception.webplugin;

import com.hh.uiperception.core.semantic.SemanticNode;
import com.hh.uiperception.core.semantic.SemanticRole;

import org.junit.Test;

import static org.junit.Assert.*;

public final class WebJsonParserTest {

    // --- Role mapping ---

    @Test
    public void mapsStandardRoles() {
        SemanticNode node = parseSingleNode("{\"role\":\"button\",\"name\":\"Click\",\"states\":[],\"bounds\":[0,0,100,50],\"children\":[]}");
        assertEquals(SemanticRole.BUTTON, node.role());
        assertEquals("Click", node.name());
    }

    @Test
    public void mapsTextboxRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"textbox\",\"name\":\"Email\",\"states\":[],\"bounds\":[0,0,200,40],\"children\":[]}");
        assertEquals(SemanticRole.TEXTBOX, node.role());
    }

    @Test
    public void mapsSearchboxRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"searchbox\",\"name\":\"Search\",\"states\":[],\"bounds\":[0,0,200,40],\"children\":[]}");
        assertEquals(SemanticRole.SEARCHBOX, node.role());
    }

    @Test
    public void mapsComboboxRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"combobox\",\"name\":\"Country\",\"states\":[],\"bounds\":[0,0,200,40],\"children\":[]}");
        assertEquals(SemanticRole.COMBOBOX, node.role());
    }

    @Test
    public void mapsListboxRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"listbox\",\"name\":\"Options\",\"states\":[],\"bounds\":[0,0,200,100],\"children\":[]}");
        assertEquals(SemanticRole.LISTBOX, node.role());
    }

    @Test
    public void mapsSpinbuttonRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"spinbutton\",\"name\":\"Count\",\"states\":[],\"bounds\":[0,0,100,40],\"children\":[]}");
        assertEquals(SemanticRole.SPINBUTTON, node.role());
    }

    @Test
    public void mapsTableRoles() {
        SemanticNode node = parseSingleNode("{\"role\":\"table\",\"name\":\"Data\",\"states\":[],\"bounds\":[0,0,300,200],\"children\":[]}");
        assertEquals(SemanticRole.TABLE_ROLE, node.role());

        node = parseSingleNode("{\"role\":\"row\",\"name\":\"\",\"states\":[],\"bounds\":[0,0,300,30],\"children\":[]}");
        assertEquals(SemanticRole.ROW, node.role());

        node = parseSingleNode("{\"role\":\"cell\",\"name\":\"Value\",\"states\":[],\"bounds\":[0,0,100,30],\"children\":[]}");
        assertEquals(SemanticRole.CELL, node.role());

        node = parseSingleNode("{\"role\":\"columnheader\",\"name\":\"Name\",\"states\":[],\"bounds\":[0,0,100,30],\"children\":[]}");
        assertEquals(SemanticRole.COLUMNHEADER, node.role());
    }

    @Test
    public void mapsUnknownRoleToGeneric() {
        SemanticNode node = parseSingleNode("{\"role\":\"unknownrole\",\"name\":\"X\",\"states\":[],\"bounds\":[0,0,10,10],\"children\":[]}");
        assertEquals(SemanticRole.GENERIC, node.role());
    }

    // --- States ---

    @Test
    public void parsesStates() {
        SemanticNode node = parseSingleNode("{\"role\":\"button\",\"name\":\"X\",\"states\":[\"disabled\",\"expanded\"],\"bounds\":[0,0,100,50],\"children\":[]}");
        assertTrue(node.states().contains("disabled"));
        assertTrue(node.states().contains("expanded"));
    }

    // --- Bounds ---

    @Test
    public void parsesBounds() {
        SemanticNode node = parseSingleNode("{\"role\":\"button\",\"name\":\"X\",\"states\":[],\"bounds\":[10,20,110,70],\"children\":[]}");
        assertNotNull(node.bounds());
        assertEquals(10, node.bounds().left());
        assertEquals(20, node.bounds().top());
        assertEquals(110, node.bounds().right());
        assertEquals(70, node.bounds().bottom());
    }

    // --- Children ---

    @Test
    public void parsesChildren() {
        String json = "{\"role\":\"screen\",\"name\":\"\",\"states\":[],\"bounds\":[],\"children\":[" +
                "{\"role\":\"button\",\"name\":\"A\",\"states\":[],\"bounds\":[0,0,50,30],\"children\":[]}," +
                "{\"role\":\"text\",\"name\":\"hello\",\"states\":[],\"bounds\":[],\"children\":[]}" +
                "]}";
        SemanticNode root = WebJsonParser.parse(wrapJson(json));
        assertNotNull(root);
        assertEquals(2, root.children().size());
        assertEquals(SemanticRole.BUTTON, root.children().get(0).role());
        assertEquals(SemanticRole.TEXT, root.children().get(1).role());
    }

    // --- Null / empty ---

    @Test
    public void returnsNullForEmptyInput() {
        assertNull(WebJsonParser.parse(""));
        assertNull(WebJsonParser.parse(null));
    }

    // --- Helpers ---

    private static SemanticNode parseSingleNode(String nodeJson) {
        String wrapped = wrapJson(nodeJson);
        SemanticNode root = WebJsonParser.parse(wrapped);
        assertNotNull("Parser returned null for: " + nodeJson, root);
        return root;
    }

    private static String wrapJson(String rootJson) {
        return "{\"url\":\"https://example.com\",\"title\":\"Test\",\"root\":" + rootJson + "}";
    }
}
