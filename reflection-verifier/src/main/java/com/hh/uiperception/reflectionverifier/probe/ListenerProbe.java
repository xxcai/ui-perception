package com.hh.uiperception.reflectionverifier.probe;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 通过反射探测 ListView / RecyclerView 是否设置了 item 级别的点击监听器。
 *
 * 探测项：
 * 1. AdapterView.mOnItemClickListener
 * 2. AdapterView.mOnItemLongClickListener
 * 3. AdapterView.mOnItemSelectedListener
 * 4. RecyclerView.mOnItemTouchListeners (List)
 * 5. View.hasOnClickListeners() (公开 API)
 */
public final class ListenerProbe {

    private static final String TAG = "ListenerProbe";

    private ListenerProbe() {
    }

    // ---- AdapterView 探测 ----

    /**
     * 探测 AdapterView 的指定 listener 字段。
     *
     * 注意：Android hidden API 限制会导致某些字段即使存在也无法反射访问。
     * 例如 mOnItemLongClickListener 标记为 api=max-target-o，targetSdk > 26 时会被拒绝。
     * 拒绝时 getDeclaredField() 会抛出 NoSuchFieldException，需要区分 "真的不存在" 和 "被限制访问"。
     *
     * @param view      AdapterView 实例（ListView / GridView 等）
     * @param fieldName 字段名，如 "mOnItemClickListener"
     * @return 探测结果
     */
    public static ProbeResult probeAdapterViewListener(AdapterView<?> view, String fieldName) {
        try {
            Field f = AdapterView.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            Object value = f.get(view);
            boolean detected = value != null;
            Log.d(TAG, fieldName + " on " + view.getClass().getSimpleName()
                    + ": found=true, detected=" + detected);
            return new ProbeResult("AdapterView." + fieldName, true, detected,
                    fieldName, "android.widget.AdapterView", null);
        } catch (NoSuchFieldException e) {
            // hidden API 限制也会抛出 NoSuchFieldException，字段实际存在但访问被拒绝
            Log.w(TAG, fieldName + ": blocked by hidden API restriction or field not found");
            return new ProbeResult("AdapterView." + fieldName, false, false,
                    null, null, "BLOCKED_BY_HIDDEN_API: " + fieldName);
        } catch (IllegalAccessException e) {
            Log.e(TAG, fieldName + ": illegal access");
            return new ProbeResult("AdapterView." + fieldName, false, false,
                    null, null, "IllegalAccessException: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, fieldName + ": unexpected error", e);
            return new ProbeResult("AdapterView." + fieldName, false, false,
                    null, null, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * 对 AdapterView 执行全部 listener 探测。
     */
    public static List<ProbeResult> probeAllAdapterViewListeners(AdapterView<?> view) {
        List<ProbeResult> results = new ArrayList<>();
        results.add(probeAdapterViewListener(view, "mOnItemClickListener"));
        results.add(probeAdapterViewListener(view, "mOnItemLongClickListener"));
        results.add(probeAdapterViewListener(view, "mOnItemSelectedListener"));
        return results;
    }

    // ---- RecyclerView 探测 ----

    /**
     * 探测 RecyclerView 的 mOnItemTouchListeners 字段。
     * 向上遍历 class hierarchy 查找字段，以兼容 RecyclerView 子类。
     *
     * @param rv RecyclerView 实例
     * @return 探测结果
     */
    public static ProbeResult probeRecyclerViewItemTouchListeners(RecyclerView rv) {
        try {
            Class<?> cls = rv.getClass();
            Field foundField = null;

            // 向上遍历 class hierarchy 查找字段
            while (cls != null) {
                try {
                    foundField = cls.getDeclaredField("mOnItemTouchListeners");
                    break;
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }

            if (foundField == null) {
                Log.e(TAG, "mOnItemTouchListeners: not found in class hierarchy of "
                        + rv.getClass().getName());
                return new ProbeResult("RecyclerView.mOnItemTouchListeners", false, false,
                        null, null, "Field not found in class hierarchy");
            }

            foundField.setAccessible(true);
            Object value = foundField.get(rv);

            boolean detected = false;
            int size = 0;
            if (value instanceof List) {
                size = ((List<?>) value).size();
                detected = size > 0;
            }

            Log.d(TAG, "mOnItemTouchListeners on " + rv.getClass().getSimpleName()
                    + ": found=true, declaringClass=" + foundField.getDeclaringClass().getName()
                    + ", listSize=" + size + ", detected=" + detected);

            return new ProbeResult("RecyclerView.mOnItemTouchListeners", true, detected,
                    foundField.getName(), foundField.getDeclaringClass().getName(),
                    null);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "mOnItemTouchListeners: illegal access");
            return new ProbeResult("RecyclerView.mOnItemTouchListeners", false, false,
                    null, null, "IllegalAccessException: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "mOnItemTouchListeners: unexpected error", e);
            return new ProbeResult("RecyclerView.mOnItemTouchListeners", false, false,
                    null, null, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ---- View 公开 API 探测 ----

    /**
     * 探测 View 是否设置了 OnClickListener。
     * 使用公开 API View.hasOnClickListeners()，无需反射。
     */
    public static ProbeResult probeHasOnClickListeners(View view) {
        boolean detected = view.hasOnClickListeners();
        Log.d(TAG, "hasOnClickListeners on " + view.getClass().getSimpleName()
                + ": detected=" + detected);
        return new ProbeResult("View.hasOnClickListeners()", true, detected,
                "hasOnClickListeners()", "android.view.View", null);
    }

    // ---- 完整探测报告 ----

    /**
     * 对一个 View 执行完整的 listener 探测。
     * 根据控件类型自动选择探测项。
     *
     * @param view     待探测的 View
     * @param scenario 场景描述
     * @param expectValues 期望的 valueDetected 值，与探测结果一一对应
     */
    public static ProbeReport fullProbe(View view, String scenario, boolean... expectValues) {
        List<ProbeResult> results = new ArrayList<>();

        if (view instanceof AdapterView) {
            results.addAll(probeAllAdapterViewListeners((AdapterView<?>) view));
        }
        if (view instanceof RecyclerView) {
            results.add(probeRecyclerViewItemTouchListeners((RecyclerView) view));
        }
        results.add(probeHasOnClickListeners(view));

        boolean allFieldsFound = true;
        for (ProbeResult r : results) {
            if (!r.fieldFound) {
                allFieldsFound = false;
                break;
            }
        }

        boolean passed = true;
        for (int i = 0; i < results.size() && i < expectValues.length; i++) {
            if (results.get(i).valueDetected != expectValues[i]) {
                passed = false;
                break;
            }
        }

        return new ProbeReport(view.getClass().getName(), scenario, results, allFieldsFound, passed);
    }
}
