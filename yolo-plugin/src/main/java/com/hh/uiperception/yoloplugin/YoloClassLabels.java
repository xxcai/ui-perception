package com.hh.uiperception.yoloplugin;

/**
 * 21 类 Android UI 检测模型的类别名和常量。
 * 与 X-OmniClaw 的 UiYoloClassLabels 保持一致。
 */
public final class YoloClassLabels {

    public static final String MODEL_ASSET_NAME = "android_ui_detection.onnx";
    public static final int INPUT_SIZE = 640;
    public static final float DEFAULT_CONFIDENCE = 0.15f;
    public static final float DEFAULT_IOU = 0.45f;
    public static final int MAX_DETECTIONS = 50;
    public static final int NUM_CLASSES = 21;

    private static final String[] NAMES_EN = {
            "BackgroundImage", "Bottom_Navigation", "Card", "CheckBox", "Checkbox",
            "CheckedTextView", "Drawer", "EditText", "Icon", "Image",
            "Map", "Modal", "Multi_Tab", "PageIndicator", "Remember",
            "Spinner", "Switch", "Text", "TextButton", "Toolbar", "UpperTaskBar"
    };

    private static final String[] NAMES_ZH = {
            "背景图", "底部导航", "卡片", "复选框", "复选框", "可选中文本",
            "抽屉", "输入框", "图标", "图片", "地图", "弹窗/模态", "多标签", "页面指示器",
            "记住选项", "下拉框", "开关", "文本", "文字按钮", "工具栏", "顶栏"
    };

    private YoloClassLabels() {}

    public static String displayName(int classId) {
        if (classId < 0 || classId >= NUM_CLASSES) {
            return "未知类别#" + classId;
        }
        return NAMES_ZH[classId] + "/" + NAMES_EN[classId];
    }

    public static String englishName(int classId) {
        if (classId < 0 || classId >= NUM_CLASSES) {
            return "Unknown#" + classId;
        }
        return NAMES_EN[classId];
    }
}
