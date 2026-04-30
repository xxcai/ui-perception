package com.hh.uiperception.nativeplugin.semantic;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * 将 native View XML 解析为原始 ViewNode 树。
 */
public final class NativeViewXmlParser {

    private NativeViewXmlParser() {
    }

    public static NativeViewNode parse(String rawXml) {
        if (rawXml == null || rawXml.trim().isEmpty()) {
            return null;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            configureFactory(factory);
            Document document = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(rawXml)));
            Element rootElement = document.getDocumentElement();
            Element rootNodeElement = "node".equals(rootElement.getTagName())
                    ? rootElement : firstChildNode(rootElement);
            return rootNodeElement == null ? null : parseNode(rootNodeElement);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse native view XML", e);
        }
    }

    private static void configureFactory(DocumentBuilderFactory factory) {
        factory.setIgnoringComments(true);
        factory.setCoalescing(true);
        factory.setExpandEntityReferences(false);
        trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        trySetFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignored) {
            // Android/JVM XML parser feature support differs; keep parser usable when a hardening flag is unsupported.
        }
    }

    private static Element firstChildNode(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && "node".equals(((Element) child).getTagName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private static NativeViewNode parseNode(Element element) {
        NativeViewNode.Builder builder = builderFrom(element);
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && "node".equals(((Element) child).getTagName())) {
                builder.addChild(parseNode((Element) child));
            }
        }
        return builder.build();
    }

    private static NativeViewNode.Builder builderFrom(Element element) {
        return NativeViewNode.builder()
                .className(attr(element, "class"))
                .resourceId(attr(element, "resource-id"))
                .text(attr(element, "text"))
                .contentDescription(firstText(attr(element, "desc"), attr(element, "content-desc")))
                .bounds(NativeBounds.parse(attr(element, "bounds")))
                .clickable(parseBoolean(attr(element, "clickable")))
                .enabled(parseBoolean(attr(element, "enabled"), true))
                .checked(parseBoolean(attr(element, "checked")))
                .selected(parseBoolean(attr(element, "selected")))
                .focused(parseBoolean(attr(element, "focused")))
                .scrollable(parseBoolean(attr(element, "scrollable")))
                .password(parseBoolean(attr(element, "password")));
    }

    private static String attr(Element element, String name) {
        return element.hasAttribute(name) ? element.getAttribute(name) : "";
    }

    private static String firstText(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second == null ? "" : second;
    }

    private static boolean parseBoolean(String value) {
        return parseBoolean(value, false);
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}
