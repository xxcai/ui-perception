package com.hh.uiperception.nativeplugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.hh.uiperception.core.TransformRequest;
import com.hh.uiperception.core.TransformResult;

import org.junit.Test;

public final class NativeXmlTransformToolTest {

    @Test
    public void transformsRawXmlToSemanticSnapshot() {
        String xml = ""
                + "<hierarchy activity=\"DemoActivity\">"
                + "<node class=\"android.widget.FrameLayout\" bounds=\"[0,0][1080,2400]\" enabled=\"true\">"
                + "<node class=\"android.widget.TextView\" text=\"邮件\" bounds=\"[32,48][160,112]\" />"
                + "<node class=\"android.widget.TextView\" text=\"提交\" clickable=\"true\" bounds=\"[800,100][1000,180]\" />"
                + "</node>"
                + "</hierarchy>";
        TransformRequest request = new TransformRequest(
                "native_home_mail",
                "native_xml",
                "text/xml",
                xml,
                "DemoActivity"
        );

        TransformResult result = NativeXmlTransformTool.transform(request);

        assertTrue(result.isSuccess());
        assertEquals("native_semantic_snapshot", result.toolName());
        assertEquals("native_xml", result.sourceToolName());
        assertEquals("text/yaml", result.contentType());
        assertEquals(""
                + "- generic:\n"
                + "  - text \"邮件\"\n"
                + "  - button \"提交\" [ref=n1] [bounds=800,100,1000,180]", result.content());
    }
}
