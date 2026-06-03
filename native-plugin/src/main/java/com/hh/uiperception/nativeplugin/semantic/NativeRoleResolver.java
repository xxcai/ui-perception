package com.hh.uiperception.nativeplugin.semantic;

import com.hh.uiperception.core.semantic.*;

/**
 * Android native role 推导入口。规则集中在这里，方便后续版本化演进。
 */
public final class NativeRoleResolver {

    public static final String ROLE_VERSION = "android-role-v1";

    private NativeRoleResolver() {
    }

    public static RoleDecision resolve(NativeViewNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }

        String className = node.className();
        String simpleName = simpleName(className);

        RoleDecision classDecision = resolveByClass(simpleName, className);
        RoleDecision adjusted = adjustByAttributes(node, classDecision);
        if (adjusted != null) {
            return adjusted;
        }
        if (classDecision != null) {
            return classDecision;
        }
        if (node.hasText() || node.hasContentDescription()) {
            return new RoleDecision(SemanticRole.TEXT, "text-or-desc-fallback", 0.55);
        }
        return new RoleDecision(SemanticRole.GENERIC, "generic-fallback", 0.3);
    }

    public static String resolveName(NativeViewNode node, SemanticRole role) {
        if (node == null) {
            return "";
        }
        if (node.hasText()) {
            return node.text();
        }
        if (node.hasContentDescription()) {
            return node.contentDescription();
        }
        if (role != SemanticRole.TEXT && node.hasResourceId()) {
            return readableResourceName(node.resourceId());
        }
        return "";
    }

    private static RoleDecision resolveByClass(String simpleName, String className) {
        if (simpleName.isEmpty()) {
            return null;
        }
        if (matches(simpleName, "DecorView") || className.endsWith(".DecorView")) {
            return new RoleDecision(SemanticRole.SCREEN, "class:root", 0.9);
        }
        if (containsAny(simpleName, "Toolbar", "ActionBar", "AppBar")) {
            return new RoleDecision(SemanticRole.TOOLBAR, "class:toolbar", 0.9);
        }
        if (containsAny(simpleName, "TabLayout")) {
            return new RoleDecision(SemanticRole.TABBAR, "class:tabbar", 0.9);
        }
        if (containsAny(simpleName, "RecyclerView", "ListView")) {
            return new RoleDecision(SemanticRole.LIST, "class:list", 0.9);
        }
        if (containsAny(simpleName, "GridView")) {
            return new RoleDecision(SemanticRole.GRID, "class:grid", 0.9);
        }
        if (containsAny(simpleName, "ScrollView")) {
            return new RoleDecision(SemanticRole.SCROLL, "class:scroll", 0.9);
        }
        if (containsAny(simpleName, "CardView")) {
            return new RoleDecision(SemanticRole.CARD, "class:card", 0.85);
        }
        if (containsAny(simpleName, "Dialog")) {
            return new RoleDecision(SemanticRole.DIALOG, "class:dialog", 0.85);
        }
        if (containsAny(simpleName, "BottomSheet")) {
            return new RoleDecision(SemanticRole.SHEET, "class:sheet", 0.85);
        }
        if (containsAny(simpleName, "EditText", "TextInputEditText")) {
            return new RoleDecision(SemanticRole.INPUT, "class:input", 0.95);
        }
        if (containsAny(simpleName, "CheckBox")) {
            return new RoleDecision(SemanticRole.CHECKBOX, "class:checkbox", 0.95);
        }
        if (containsAny(simpleName, "RadioButton")) {
            return new RoleDecision(SemanticRole.RADIO, "class:radio", 0.95);
        }
        if (containsAny(simpleName, "Switch")) {
            return new RoleDecision(SemanticRole.SWITCH, "class:switch", 0.95);
        }
        if (containsAny(simpleName, "SeekBar", "Slider")) {
            return new RoleDecision(SemanticRole.SLIDER, "class:slider", 0.9);
        }
        if (containsAny(simpleName, "Spinner", "NumberPicker", "DatePicker", "TimePicker")) {
            return new RoleDecision(SemanticRole.PICKER, "class:picker", 0.9);
        }
        if (containsAny(simpleName, "ProgressBar")) {
            return new RoleDecision(SemanticRole.PROGRESS, "class:progress", 0.9);
        }
        if (containsAny(simpleName, "Button")) {
            return new RoleDecision(SemanticRole.BUTTON, "class:button", 0.95);
        }
        if (containsAny(simpleName, "ImageView")) {
            return new RoleDecision(SemanticRole.IMAGE, "class:image", 0.85);
        }
        if (containsAny(simpleName, "TextView")) {
            return new RoleDecision(SemanticRole.TEXT, "class:text", 0.85);
        }
        if (containsAny(simpleName, "Layout", "ViewGroup", "FrameLayout", "LinearLayout", "RelativeLayout", "ConstraintLayout")) {
            return new RoleDecision(SemanticRole.GENERIC, "class:container", 0.6);
        }
        return null;
    }

    private static RoleDecision adjustByAttributes(NativeViewNode node, RoleDecision classDecision) {
        SemanticRole role = classDecision == null ? null : classDecision.role();
        boolean interactiveGeneric = role == null
                || role == SemanticRole.TEXT
                || role == SemanticRole.IMAGE
                || role == SemanticRole.GENERIC;
        if ((node.clickable() || node.longClickable()) && interactiveGeneric) {
            return new RoleDecision(SemanticRole.BUTTON, "attribute:clickable", 0.8);
        }
        if (node.scrollable() && (role == null || role == SemanticRole.GENERIC)) {
            return new RoleDecision(SemanticRole.SCROLL, "attribute:scrollable", 0.7);
        }
        if (node.editable() && (role == null || role == SemanticRole.GENERIC || role == SemanticRole.TEXT)) {
            return new RoleDecision(SemanticRole.INPUT, "attribute:editable", 0.75);
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
