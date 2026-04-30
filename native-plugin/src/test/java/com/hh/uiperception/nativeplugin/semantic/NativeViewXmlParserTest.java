package com.hh.uiperception.nativeplugin.semantic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class NativeViewXmlParserTest {

    @Test
    public void parsesNativeViewTreeAttributesAndChildren() {
        String xml = ""
                + "<hierarchy activity=\"DemoActivity\">"
                + "<node index=\"0\" class=\"android.widget.FrameLayout\" bounds=\"[0,0][1080,2400]\" clickable=\"false\" enabled=\"true\">"
                + "<node index=\"1\" class=\"android.widget.TextView\" text=\"  邮件  \" resource-id=\"com.demo:id/title\" bounds=\"[32,48][160,112]\" enabled=\"true\" />"
                + "<node index=\"2\" class=\"android.widget.ImageButton\" desc=\"搜索\" clickable=\"true\" enabled=\"true\" bounds=\"[960,40][1040,120]\" />"
                + "<node index=\"3\" class=\"androidx.recyclerview.widget.RecyclerView\" scrollable=\"true\" enabled=\"true\" bounds=\"[0,160][1080,1900]\">"
                + "<node index=\"4\" class=\"android.widget.TextView\" text=\"张三\" selected=\"true\" bounds=\"[32,180][180,230]\" />"
                + "</node>"
                + "</node>"
                + "</hierarchy>";

        NativeViewNode root = NativeViewXmlParser.parse(xml);

        assertNotNull(root);
        assertEquals("android.widget.FrameLayout", root.className());
        assertEquals(3, root.children().size());
        assertFalse(root.clickable());
        assertTrue(root.enabled());
        assertEquals("0,0,1080,2400", root.bounds().toSnapshotValue());

        NativeViewNode title = root.children().get(0);
        assertEquals("android.widget.TextView", title.className());
        assertEquals("邮件", title.text());
        assertEquals("com.demo:id/title", title.resourceId());

        NativeViewNode search = root.children().get(1);
        assertEquals("android.widget.ImageButton", search.className());
        assertEquals("搜索", search.contentDescription());
        assertTrue(search.clickable());
        assertEquals(1000, search.bounds().centerX());
        assertEquals(80, search.bounds().centerY());

        NativeViewNode list = root.children().get(2);
        assertTrue(list.scrollable());
        assertEquals(1, list.children().size());
        assertTrue(list.children().get(0).selected());
    }

    @Test
    public void supportsContentDescAlias() {
        String xml = "<node class=\"android.widget.ImageButton\" content-desc=\"更多\" bounds=\"[1,2][3,4]\" />";

        NativeViewNode node = NativeViewXmlParser.parse(xml);

        assertNotNull(node);
        assertEquals("更多", node.contentDescription());
    }

    @Test
    public void returnsNullForBlankXml() {
        assertNull(NativeViewXmlParser.parse(" "));
    }
}
