package com.hh.uiperception.nativeplugin.semantic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class NativeSemanticSnapshotGeneratorTest {

    @Test
    public void generatesSnapshotFromRawNativeXml() {
        String xml = ""
                + "<hierarchy activity=\"DemoActivity\">"
                + "<node class=\"android.widget.FrameLayout\" bounds=\"[0,0][1080,2400]\" enabled=\"true\">"
                + "<node class=\"android.widget.TextView\" text=\"邮件\" bounds=\"[32,48][160,112]\" />"
                + "<node class=\"android.widget.TextView\" text=\"提交\" clickable=\"true\" enabled=\"true\" bounds=\"[800,100][1000,180]\" />"
                + "<node class=\"androidx.recyclerview.widget.RecyclerView\" scrollable=\"true\" enabled=\"true\" bounds=\"[0,160][1080,1900]\">"
                + "<node class=\"android.widget.LinearLayout\" bounds=\"[0,160][1080,320]\">"
                + "<node class=\"android.widget.TextView\" text=\"吴示例\" bounds=\"[32,180][180,230]\" />"
                + "<node class=\"android.widget.Button\" text=\"拨打\" clickable=\"true\" bounds=\"[920,180][1040,260]\" />"
                + "</node>"
                + "</node>"
                + "</node>"
                + "</hierarchy>";

        String snapshot = NativeSemanticSnapshotGenerator.generate(xml);

        assertEquals(""
                + "- generic:\n"
                + "  - text \"邮件\"\n"
                + "  - button \"提交\" [ref=n1] [bounds=800,100,1000,180]\n"
                + "  - list [scrollable] [ref=n2] [bounds=0,160,1080,1900]:\n"
                + "    - listitem [clickable-guessed]:\n"
                + "      - text \"吴示例\"\n"
                + "      - button \"拨打\" [ref=n3] [bounds=920,180,1040,260]", snapshot);
    }

    @Test
    public void supportsRendererOptions() {
        String xml = ""
                + "<node class=\"android.widget.LinearLayout\" bounds=\"[0,0][100,100]\">"
                + "<node class=\"android.widget.TextView\" text=\"邮件\" bounds=\"[1,2][3,4]\" />"
                + "</node>";

        String snapshot = NativeSemanticSnapshotGenerator.generate(
                xml,
                NativeSnapshotRenderOptions.builder().boxes(true).build()
        );

        assertEquals("- text \"邮件\" [bounds=1,2,3,4]", snapshot);
    }

    @Test
    public void marksUnnamedImageButtonForVisualDescription() {
        String xml = ""
                + "<node class=\"android.widget.FrameLayout\" bounds=\"[0,0][1080,2400]\">"
                + "<node class=\"android.widget.ImageButton\" clickable=\"true\" bounds=\"[900,120][1000,220]\" />"
                + "</node>";

        String snapshot = NativeSemanticSnapshotGenerator.generate(xml);

        assertEquals(""
                + "- button [needs_visual_desc] [ref=n1] [bounds=900,120,1000,220]", snapshot);
    }

    @Test
    public void listViewItemClickListenerProducesClickableInferredListItems() {
        String xml = ""
                + "<node class=\"android.widget.ListView\""
                + " has-item-click-listener=\"true\""
                + " scrollable=\"true\""
                + " bounds=\"[0,100][1080,900]\">"
                + "<node class=\"android.widget.LinearLayout\" bounds=\"[0,100][1080,260]\">"
                + "<node class=\"android.widget.TextView\" text=\"华为 Wi-Fi\" bounds=\"[42,120][400,180]\" />"
                + "<node class=\"android.widget.Button\" text=\"详情\" clickable=\"true\" bounds=\"[900,160][1040,240]\" />"
                + "</node>"
                + "</node>";

        String snapshot = NativeSemanticSnapshotGenerator.generate(xml);

        assertEquals(""
                + "- list [scrollable] [ref=n1] [bounds=0,100,1080,900]:\n"
                + "  - listitem [clickable-inferred] [ref=n2] [bounds=0,100,1080,260]:\n"
                + "    - text \"华为 Wi-Fi\"\n"
                + "    - button \"详情\" [ref=n3] [bounds=900,160,1040,240]", snapshot);
    }

    @Test
    public void recyclerViewItemTouchListenerProducesClickableInferredListItems() {
        String xml = ""
                + "<node class=\"androidx.recyclerview.widget.RecyclerView\""
                + " has-item-touch-listener=\"true\""
                + " scrollable=\"true\""
                + " bounds=\"[0,160][1080,1900]\">"
                + "<node class=\"android.widget.LinearLayout\" bounds=\"[0,160][1080,320]\">"
                + "<node class=\"android.widget.TextView\" text=\"张三\" bounds=\"[42,180][180,230]\" />"
                + "<node class=\"android.widget.TextView\" text=\"会议纪要\" bounds=\"[42,230][420,290]\" />"
                + "</node>"
                + "</node>";

        String snapshot = NativeSemanticSnapshotGenerator.generate(xml);

        assertEquals(""
                + "- list [scrollable] [ref=n1] [bounds=0,160,1080,1900]:\n"
                + "  - listitem [clickable-inferred] [ref=n2] [bounds=0,160,1080,320]:\n"
                + "    - text \"张三\"\n"
                + "    - text \"会议纪要\"", snapshot);
    }
}
