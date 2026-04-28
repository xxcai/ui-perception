package com.hh.uiperception.capture.nativexml;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Checkable;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 进程内 View 层级遍历器。
 * 遍历 Activity 的 DecorView，输出完整的 XML 字符串。
 * 不做节点上限截断，不裁剪属性，为 Phase 3 提供完整原始数据。
 * 必须在 UI 线程调用。
 */
public final class ViewHierarchyDumper {

    private ViewHierarchyDumper() {
    }

    /**
     * 抓取给定 Activity 的 View 层级，输出 XML。
     */
    public static DumpResult dump(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return DumpResult.error("Activity 不可用");
        }
        View decorView = activity.getWindow() != null
                ? activity.getWindow().getDecorView() : null;
        if (decorView == null) {
            return DumpResult.error("Activity 没有 DecorView");
        }

        StringBuilder xml = new StringBuilder();
        DumpState state = new DumpState();
        xml.append("<hierarchy activity=\"")
           .append(escape(activity.getClass().getName()))
           .append("\">");
        appendNode(xml, decorView, state);
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
        appendOptional(xml, "desc", extractDesc(view));
        xml.append(" bounds=\"").append(escape(extractBounds(view))).append("\"");
        xml.append(" clickable=\"").append(view.isClickable()).append("\"");
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
