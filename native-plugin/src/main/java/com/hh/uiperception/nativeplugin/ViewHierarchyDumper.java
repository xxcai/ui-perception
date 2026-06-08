package com.hh.uiperception.nativeplugin;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.Checkable;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 进程内 View 层级遍历器。
 * 遍历 Activity 的 DecorView，输出完整的 XML 字符串。
 * 不做节点上限截断，不裁剪属性，为 Phase 3 提供完整原始数据。
 * 必须在 UI 线程调用。
 */
public final class ViewHierarchyDumper {

    private static final String RECYCLER_VIEW_CLASS_NAME = "androidx.recyclerview.widget.RecyclerView";
    private static Field adapterViewItemClickListenerField;
    private static boolean adapterViewItemClickListenerFieldResolved;
    private static Field recyclerViewItemTouchListenersField;
    private static boolean recyclerViewItemTouchListenersFieldResolved;
    private static final Map<Class<?>, Boolean> onTouchEventOverrideCache = new HashMap<>();

    private ViewHierarchyDumper() {
    }

    /**
     * 抓取给定 Activity 的 View 层级，输出 XML。
     * 自动检测焦点窗口（能覆盖 Dialog 等弹窗场景）。
     */
    public static DumpResult dump(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return DumpResult.error("Activity 不可用");
        }

        View rootView = WindowManagerHelper.getFocusedWindowView(activity);
        if (rootView == null) {
            rootView = activity.getWindow() != null ? activity.getWindow().getDecorView() : null;
        }
        if (rootView == null) {
            return DumpResult.error("Activity 没有 DecorView");
        }

        StringBuilder xml = new StringBuilder();
        DumpState state = new DumpState();
        xml.append("<hierarchy activity=\"")
           .append(escape(activity.getClass().getName()))
           .append("\">");
        appendNode(xml, rootView, state);
        xml.append("</hierarchy>");

        return DumpResult.success(xml.toString(), activity.getClass().getName(), state.nodeCount);
    }

    // --- 遍历 ---

    private static void appendNode(StringBuilder xml, View view, DumpState state) {
        if (view == null) {
            return;
        }
        if (!isMeaningful(view)) {
            return;
        }

        int index = state.nodeCount++;
        xml.append("<node");
        xml.append(" index=\"").append(index).append("\"");
        xml.append(" class=\"").append(escape(view.getClass().getName())).append("\"");
        appendOptional(xml, "resource-id", resolveResourceId(view));
        appendOptional(xml, "text", extractText(view));
        appendOptional(xml, "hint", extractHint(view));
        appendOptional(xml, "desc", extractDesc(view));
        xml.append(" bounds=\"").append(escape(extractBounds(view))).append("\"");
        xml.append(" clickable=\"").append(view.isClickable()).append("\"");
        xml.append(" has-onclick-listener=\"").append(view.hasOnClickListeners()).append("\"");
        appendContainerClickSignals(xml, view);
        xml.append(" overrides-onTouchEvent=\"").append(overridesOnTouchEvent(view)).append("\"");
        xml.append(" enabled=\"").append(view.isEnabled()).append("\"");
        xml.append(" focusable=\"").append(view.isFocusable()).append("\"");
        xml.append(" checked=\"").append(extractChecked(view)).append("\"");
        xml.append(" scrollable=\"").append(isScrollable(view)).append("\"");
        xml.append(" selected=\"").append(view.isSelected()).append("\"");
        xml.append(">");

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                appendNode(xml, group.getChildAt(i), state);
            }
        }

        xml.append("</node>");
    }

    // --- 过滤 ---

    private static boolean isMeaningful(View view) {
        if (view.getVisibility() != View.VISIBLE) {
            return false;
        }
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            return false;
        }
        if (view.getAlpha() <= 0f) {
            return false;
        }
        ViewParent parent = view.getParent();
        return parent != null || view.getRootView() == view;
    }

    // --- 属性提取 ---

    private static String extractText(View view) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (!TextUtils.isEmpty(text)) {
                return text.toString();
            }
        }
        return "";
    }

    private static String extractHint(View view) {
        if (view instanceof TextView) {
            CharSequence hint = ((TextView) view).getHint();
            if (!TextUtils.isEmpty(hint)) {
                return hint.toString();
            }
        }
        return "";
    }

    private static String extractDesc(View view) {
        CharSequence cd = view.getContentDescription();
        return cd != null ? cd.toString() : "";
    }

    private static String resolveResourceId(View view) {
        int id = view.getId();
        if (id == View.NO_ID) {
            return "";
        }
        try {
            return view.getResources().getResourceName(id);
        } catch (Exception e) {
            return String.valueOf(id);
        }
    }

    private static String extractBounds(View view) {
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        return "[" + loc[0] + "," + loc[1] + "]"
             + "[" + (loc[0] + view.getWidth()) + "," + (loc[1] + view.getHeight()) + "]";
    }

    private static boolean extractChecked(View view) {
        return view instanceof Checkable && ((Checkable) view).isChecked();
    }

    private static boolean isScrollable(View view) {
        return view instanceof ScrollView
                || view instanceof HorizontalScrollView
                || view instanceof ListView;
    }

    private static void appendContainerClickSignals(StringBuilder xml, View view) {
        if (view instanceof AdapterView) {
            xml.append(" has-item-click-listener=\"")
                    .append(hasAdapterViewItemClickListener((AdapterView<?>) view))
                    .append("\"");
        }
        if (isRecyclerView(view)) {
            xml.append(" has-item-touch-listener=\"")
                    .append(hasRecyclerViewItemTouchListener(view))
                    .append("\"");
        }
    }

    private static boolean hasAdapterViewItemClickListener(AdapterView<?> view) {
        Field field = adapterViewItemClickListenerField();
        if (field == null) {
            return false;
        }
        try {
            return field.get(view) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Field adapterViewItemClickListenerField() {
        if (!adapterViewItemClickListenerFieldResolved) {
            adapterViewItemClickListenerFieldResolved = true;
            adapterViewItemClickListenerField = declaredField(AdapterView.class, "mOnItemClickListener");
        }
        return adapterViewItemClickListenerField;
    }

    private static boolean hasRecyclerViewItemTouchListener(View view) {
        Field field = recyclerViewItemTouchListenersField(view.getClass());
        if (field == null) {
            return false;
        }
        try {
            Object value = field.get(view);
            return value instanceof List && !((List<?>) value).isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Field recyclerViewItemTouchListenersField(Class<?> startClass) {
        if (!recyclerViewItemTouchListenersFieldResolved) {
            recyclerViewItemTouchListenersFieldResolved = true;
            recyclerViewItemTouchListenersField =
                    declaredFieldInHierarchy(startClass, "mOnItemTouchListeners");
        }
        return recyclerViewItemTouchListenersField;
    }

    private static boolean isRecyclerView(View view) {
        Class<?> cls = view.getClass();
        while (cls != null) {
            if (RECYCLER_VIEW_CLASS_NAME.equals(cls.getName())) {
                return true;
            }
            cls = cls.getSuperclass();
        }
        return false;
    }

    /**
     * Check if a View's class overrides onTouchEvent (compared to View/ViewGroup base).
     * Uses per-class cache to avoid repeated reflection. Not synchronized — dump runs on UI thread.
     */
    private static boolean overridesOnTouchEvent(View view) {
        Class<?> cls = view.getClass();
        Boolean cached = onTouchEventOverrideCache.get(cls);
        if (cached != null) {
            return cached;
        }
        boolean result = computeOverridesOnTouchEvent(cls);
        onTouchEventOverrideCache.put(cls, result);
        return result;
    }

    private static boolean computeOverridesOnTouchEvent(Class<?> cls) {
        try {
            Method m = cls.getMethod("onTouchEvent", android.view.MotionEvent.class);
            Class<?> declaring = m.getDeclaringClass();
            return declaring != View.class && declaring != ViewGroup.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Field declaredFieldInHierarchy(Class<?> startClass, String name) {
        Class<?> cls = startClass;
        while (cls != null) {
            Field field = declaredField(cls, name);
            if (field != null) {
                return field;
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    private static Field declaredField(Class<?> cls, String name) {
        try {
            Field field = cls.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Exception ignored) {
            return null;
        }
    }

    // --- 工具方法 ---

    private static void appendOptional(StringBuilder xml, String attr, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        xml.append(" ").append(attr).append("=\"").append(escape(value)).append("\"");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                    .replace("\"", "&quot;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

    // --- 结果和状态 ---

    public static final class DumpResult {
        public final boolean success;
        public final String xml;
        public final String activityClassName;
        public final int nodeCount;
        public final String errorMessage;

        private DumpResult(boolean success, String xml, String activityClassName,
                           int nodeCount, String errorMessage) {
            this.success = success;
            this.xml = xml;
            this.activityClassName = activityClassName;
            this.nodeCount = nodeCount;
            this.errorMessage = errorMessage;
        }

        public static DumpResult success(String xml, String activityClassName, int nodeCount) {
            return new DumpResult(true, xml, activityClassName, nodeCount, null);
        }

        public static DumpResult error(String errorMessage) {
            return new DumpResult(false, null, null, 0, errorMessage);
        }
    }

    private static final class DumpState {
        int nodeCount;
    }
}
