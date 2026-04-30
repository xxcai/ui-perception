package com.hh.uiperception.nativeplugin.semantic;

/**
 * Android native role 推导入口。规则集中在这里，方便后续版本化演进。
 */
public final class NativeRoleResolver {

    public static final String ROLE_VERSION = "android-role-v1";

    private NativeRoleResolver() {
    }

    public static NativeRoleDecision resolve(NativeViewNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }

        String className = node.className();
        String simpleName = simpleName(className);

        NativeRoleDecision classDecision = resolveByClass(simpleName, className);
        NativeRoleDecision adjusted = adjustByAttributes(node, classDecision);
        if (adjusted != null) {
            return adjusted;
        }
        if (classDecision != null) {
            return classDecision;
        }
        if (node.hasText() || node.hasContentDescription()) {
            return new NativeRoleDecision(NativeSemanticRole.TEXT, "text-or-desc-fallback", 0.55);
        }
        return new NativeRoleDecision(NativeSemanticRole.GENERIC, "generic-fallback", 0.3);
    }

    public static String resolveName(NativeViewNode node, NativeSemanticRole role) {
        if (node == null) {
            return "";
        }
        if (node.hasText()) {
            return node.text();
        }
        if (node.hasContentDescription()) {
            return node.contentDescription();
        }
        if (role != NativeSemanticRole.TEXT && node.hasResourceId()) {
            return readableResourceName(node.resourceId());
        }
        return "";
    }

    private static NativeRoleDecision resolveByClass(String simpleName, String className) {
        if (simpleName.isEmpty()) {
            return null;
        }
        if (matches(simpleName, "DecorView") || className.endsWith(".DecorView")) {
            return new NativeRoleDecision(NativeSemanticRole.SCREEN, "class:root", 0.9);
        }
        if (containsAny(simpleName, "Toolbar", "ActionBar", "AppBar")) {
            return new NativeRoleDecision(NativeSemanticRole.TOOLBAR, "class:toolbar", 0.9);
        }
        if (containsAny(simpleName, "TabLayout")) {
            return new NativeRoleDecision(NativeSemanticRole.TABBAR, "class:tabbar", 0.9);
        }
        if (containsAny(simpleName, "RecyclerView", "ListView")) {
            return new NativeRoleDecision(NativeSemanticRole.LIST, "class:list", 0.9);
        }
        if (containsAny(simpleName, "GridView")) {
            return new NativeRoleDecision(NativeSemanticRole.GRID, "class:grid", 0.9);
        }
        if (containsAny(simpleName, "ScrollView")) {
            return new NativeRoleDecision(NativeSemanticRole.SCROLL, "class:scroll", 0.9);
        }
        if (containsAny(simpleName, "CardView")) {
            return new NativeRoleDecision(NativeSemanticRole.CARD, "class:card", 0.85);
        }
        if (containsAny(simpleName, "Dialog")) {
            return new NativeRoleDecision(NativeSemanticRole.DIALOG, "class:dialog", 0.85);
        }
        if (containsAny(simpleName, "BottomSheet")) {
            return new NativeRoleDecision(NativeSemanticRole.SHEET, "class:sheet", 0.85);
        }
        if (containsAny(simpleName, "EditText", "TextInputEditText")) {
            return new NativeRoleDecision(NativeSemanticRole.INPUT, "class:input", 0.95);
        }
        if (containsAny(simpleName, "CheckBox")) {
            return new NativeRoleDecision(NativeSemanticRole.CHECKBOX, "class:checkbox", 0.95);
        }
        if (containsAny(simpleName, "RadioButton")) {
            return new NativeRoleDecision(NativeSemanticRole.RADIO, "class:radio", 0.95);
        }
        if (containsAny(simpleName, "Switch")) {
            return new NativeRoleDecision(NativeSemanticRole.SWITCH, "class:switch", 0.95);
        }
        if (containsAny(simpleName, "SeekBar", "Slider")) {
            return new NativeRoleDecision(NativeSemanticRole.SLIDER, "class:slider", 0.9);
        }
        if (containsAny(simpleName, "Spinner", "NumberPicker", "DatePicker", "TimePicker")) {
            return new NativeRoleDecision(NativeSemanticRole.PICKER, "class:picker", 0.9);
        }
        if (containsAny(simpleName, "ProgressBar")) {
            return new NativeRoleDecision(NativeSemanticRole.PROGRESS, "class:progress", 0.9);
        }
        if (containsAny(simpleName, "Button")) {
            return new NativeRoleDecision(NativeSemanticRole.BUTTON, "class:button", 0.95);
        }
        if (containsAny(simpleName, "ImageView")) {
            return new NativeRoleDecision(NativeSemanticRole.IMAGE, "class:image", 0.85);
        }
        if (containsAny(simpleName, "TextView")) {
            return new NativeRoleDecision(NativeSemanticRole.TEXT, "class:text", 0.85);
        }
        if (containsAny(simpleName, "Layout", "ViewGroup", "FrameLayout", "LinearLayout", "RelativeLayout", "ConstraintLayout")) {
            return new NativeRoleDecision(NativeSemanticRole.GENERIC, "class:container", 0.6);
        }
        return null;
    }

    private static NativeRoleDecision adjustByAttributes(NativeViewNode node, NativeRoleDecision classDecision) {
        NativeSemanticRole role = classDecision == null ? null : classDecision.role();
        boolean interactiveGeneric = role == null
                || role == NativeSemanticRole.TEXT
                || role == NativeSemanticRole.IMAGE
                || role == NativeSemanticRole.GENERIC;
        if ((node.clickable() || node.longClickable()) && interactiveGeneric) {
            return new NativeRoleDecision(NativeSemanticRole.BUTTON, "attribute:clickable", 0.8);
        }
        if (node.scrollable() && (role == null || role == NativeSemanticRole.GENERIC)) {
            return new NativeRoleDecision(NativeSemanticRole.SCROLL, "attribute:scrollable", 0.7);
        }
        if (node.editable() && (role == null || role == NativeSemanticRole.GENERIC || role == NativeSemanticRole.TEXT)) {
            return new NativeRoleDecision(NativeSemanticRole.INPUT, "attribute:editable", 0.75);
        }
        return null;
    }

    private static String simpleName(String className) {
        if (className == null || className.isEmpty()) {
            return "";
        }
        int index = className.lastIndexOf('.');
        return index >= 0 ? className.substring(index + 1) : className;
    }

    private static boolean matches(String value, String expected) {
        return expected.equals(value);
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String readableResourceName(String resourceId) {
        int slash = resourceId.lastIndexOf('/');
        String tail = slash >= 0 ? resourceId.substring(slash + 1) : resourceId;
        return tail.replace('_', ' ').trim();
    }
}
