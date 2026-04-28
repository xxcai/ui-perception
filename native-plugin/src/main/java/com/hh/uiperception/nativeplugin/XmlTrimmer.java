package com.hh.uiperception.nativeplugin;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Set;

/**
 * XML 属性过滤器。
 * 接收 ViewHierarchyDumper 输出的原始 XML，只保留 5 个核心属性：
 * index、class、resource-id、text、bounds。
 * 不改变树结构，不删除任何节点。
 */
public final class XmlTrimmer {

    /** 属性白名单 */
    private static final Set<String> KEEP_ATTRS = Set.of(
            "index", "class", "resource-id", "text", "bounds"
    );

    private XmlTrimmer() {
    }

    /**
     * 对原始 XML 做属性过滤。
     *
     * @param rawXml ViewHierarchyDumper 输出的完整 XML
     * @return 只保留白名单属性的 XML
     */
    public static String trim(String rawXml) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(rawXml));

            StringWriter writer = new StringWriter(rawXml.length());
            XmlSerializer serializer = factory.newSerializer();
            serializer.setOutput(writer);

            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_TAG:
                        serializer.startTag(parser.getNamespace(), parser.getName());
                        boolean isNode = "node".equals(parser.getName());
                        // <node> 只保留白名单属性，其他标签保留全部属性
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (!isNode || KEEP_ATTRS.contains(parser.getAttributeName(i))) {
                                serializer.attribute(
                                        parser.getAttributeNamespace(i),
                                        parser.getAttributeName(i),
                                        parser.getAttributeValue(i)
                                );
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        serializer.endTag(parser.getNamespace(), parser.getName());
                        break;
                    case XmlPullParser.TEXT:
                        String text = parser.getText();
                        if (text != null) {
                            serializer.text(text);
                        }
                        break;
                }
                event = parser.next();
            }

            serializer.flush();
            return writer.toString();
        } catch (Exception e) {
            // 解析失败则返回原始 XML，不阻断抓取流程
            return rawXml;
        }
    }
}
