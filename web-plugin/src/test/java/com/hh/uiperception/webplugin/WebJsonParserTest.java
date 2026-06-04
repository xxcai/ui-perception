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

    // --- New role mappings ---

    @Test
    public void mapsArticleRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"article\",\"name\":\"Art\",\"states\":[],\"bounds\":[0,0,300,200],\"children\":[]}");
        assertEquals(SemanticRole.ARTICLE, node.role());
    }

    @Test
    public void mapsComplementaryRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"complementary\",\"name\":\"Aside\",\"states\":[],\"bounds\":[0,0,200,100],\"children\":[]}");
        assertEquals(SemanticRole.COMPLEMENTARY, node.role());
    }

    @Test
    public void mapsBlockquoteRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"blockquote\",\"name\":\"Quote\",\"states\":[],\"bounds\":[0,0,200,100],\"children\":[]}");
        assertEquals(SemanticRole.BLOCKQUOTE, node.role());
    }

    @Test
    public void mapsCaptionRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"caption\",\"name\":\"Table Caption\",\"states\":[],\"bounds\":[0,0,200,30],\"children\":[]}");
        assertEquals(SemanticRole.CAPTION, node.role());
    }

    @Test
    public void mapsGroupRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"group\",\"name\":\"Details\",\"states\":[],\"bounds\":[0,0,200,100],\"children\":[]}");
        assertEquals(SemanticRole.GROUP, node.role());
    }

    @Test
    public void mapsTermRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"term\",\"name\":\"DT\",\"states\":[],\"bounds\":[0,0,100,20],\"children\":[]}");
        assertEquals(SemanticRole.TERM, node.role());
    }

    @Test
    public void mapsDefinitionRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"definition\",\"name\":\"DD\",\"states\":[],\"bounds\":[0,0,100,20],\"children\":[]}");
        assertEquals(SemanticRole.DEFINITION, node.role());
    }

    @Test
    public void mapsSeparatorRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"separator\",\"name\":\"\",\"states\":[],\"bounds\":[0,0,200,2],\"children\":[]}");
        assertEquals(SemanticRole.SEPARATOR, node.role());
    }

    @Test
    public void mapsMeterRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"meter\",\"name\":\"Disk usage\",\"states\":[],\"bounds\":[0,0,100,20],\"children\":[]}");
        assertEquals(SemanticRole.METER, node.role());
    }

    @Test
    public void mapsOptionRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"option\",\"name\":\"Alpha\",\"states\":[\"selected\"],\"bounds\":[0,0,100,20],\"children\":[]}");
        assertEquals(SemanticRole.OPTION_ROLE, node.role());
    }

    @Test
    public void mapsStatusRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"status\",\"name\":\"Result\",\"states\":[],\"bounds\":[0,0,100,20],\"children\":[]}");
        assertEquals(SemanticRole.STATUS, node.role());
    }

    @Test
    public void mapsParagraphRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"paragraph\",\"name\":\"A paragraph\",\"states\":[],\"bounds\":[0,0,300,20],\"children\":[]}");
        assertEquals(SemanticRole.PARAGRAPH, node.role());
    }

    @Test
    public void mapsRowgroupRole() {
        SemanticNode node = parseSingleNode("{\"role\":\"rowgroup\",\"name\":\"\",\"states\":[],\"bounds\":[0,0,300,60],\"children\":[]}");
        assertEquals(SemanticRole.ROWGROUP, node.role());
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
