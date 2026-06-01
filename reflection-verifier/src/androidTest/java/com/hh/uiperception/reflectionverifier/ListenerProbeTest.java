package com.hh.uiperception.reflectionverifier;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.hh.uiperception.reflectionverifier.probe.ListenerProbe;
import com.hh.uiperception.reflectionverifier.probe.ProbeReport;
import com.hh.uiperception.reflectionverifier.probe.ProbeResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 仪器化测试：验证反射探测 ListView / RecyclerView listener 的可行性。
 * 需要在真机或模拟器上运行: ./gradlew :reflection-verifier:connectedAndroidTest
 */
@RunWith(AndroidJUnit4.class)
public class ListenerProbeTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    // ==================== AdapterView (ListView) 测试 ====================

    @Test
    public void listView_noListener_mOnItemClickListenerIsNull() {
        ListView listView = new ListView(context);

        ProbeResult result = ListenerProbe.probeAdapterViewListener(
                listView, "mOnItemClickListener");

        assertTrue("Field should be found", result.fieldFound);
        assertFalse("No listener set, should be null", result.valueDetected);
        assertNull("No error expected", result.error);
        assertEquals("android.widget.AdapterView", result.fieldDeclaringClass);
    }

    @Test
    public void listView_withOnItemClickListener_mOnItemClickListenerIsNotNull() {
        ListView listView = new ListView(context);
        listView.setOnItemClickListener((parent, view, position, id) -> {});

        ProbeResult result = ListenerProbe.probeAdapterViewListener(
                listView, "mOnItemClickListener");

        assertTrue("Field should be found", result.fieldFound);
        assertTrue("Listener was set, should be detected", result.valueDetected);
        assertNull("No error expected", result.error);
    }

    @Test
    public void listView_withOnItemLongClickListener_detected() {
        ListView listView = new ListView(context);
        listView.setOnItemLongClickListener((parent, view, position, id) -> true);

        ProbeResult result = ListenerProbe.probeAdapterViewListener(
                listView, "mOnItemLongClickListener");

        assertTrue("Field should be found", result.fieldFound);
        assertTrue("Long click listener was set, should be detected", result.valueDetected);
    }

    @Test
    public void listView_withBothClickAndLongClick_bothDetected() {
        ListView listView = new ListView(context);
        listView.setOnItemClickListener((parent, view, position, id) -> {});
        listView.setOnItemLongClickListener((parent, view, position, id) -> true);

        ProbeResult clickResult = ListenerProbe.probeAdapterViewListener(
                listView, "mOnItemClickListener");
        ProbeResult longClickResult = ListenerProbe.probeAdapterViewListener(
                listView, "mOnItemLongClickListener");

        assertTrue(clickResult.valueDetected);
        assertTrue(longClickResult.valueDetected);
    }

    @Test
    public void listView_allThreeListeners_probedAtOnce() {
        ListView listView = new ListView(context);
        listView.setOnItemClickListener((parent, view, position, id) -> {});
        listView.setOnItemLongClickListener((parent, view, position, id) -> true);
        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {}
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        List<ProbeResult> results = ListenerProbe.probeAllAdapterViewListeners(listView);

        assertEquals(3, results.size());
        assertTrue("mOnItemClickListener should be detected", results.get(0).valueDetected);
        assertTrue("mOnItemLongClickListener should be detected", results.get(1).valueDetected);
        assertTrue("mOnItemSelectedListener should be detected", results.get(2).valueDetected);
    }

    // ==================== RecyclerView 测试 ====================

    @Test
    public void recyclerView_noListener_touchListenersListIsEmpty() {
        RecyclerView rv = new RecyclerView(context);

        ProbeResult result = ListenerProbe.probeRecyclerViewItemTouchListeners(rv);

        assertTrue("Field should be found", result.fieldFound);
        assertFalse("No touch listener added, should not be detected", result.valueDetected);
        assertNull("No error expected", result.error);
    }

    @Test
    public void recyclerView_withOnItemTouchListener_detected() {
        RecyclerView rv = new RecyclerView(context);
        rv.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener());

        ProbeResult result = ListenerProbe.probeRecyclerViewItemTouchListeners(rv);

        assertTrue("Field should be found", result.fieldFound);
        assertTrue("Touch listener was added, should be detected", result.valueDetected);
    }

    @Test
    public void recyclerView_withMultipleTouchListeners_detected() {
        RecyclerView rv = new RecyclerView(context);
        rv.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener());
        rv.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent e) {
                return false;
            }
        });

        ProbeResult result = ListenerProbe.probeRecyclerViewItemTouchListeners(rv);

        assertTrue(result.fieldFound);
        assertTrue(result.valueDetected);
    }

    @Test
    public void recyclerView_removeTouchListener_noLongerDetected() {
        RecyclerView rv = new RecyclerView(context);
        RecyclerView.OnItemTouchListener listener = new RecyclerView.SimpleOnItemTouchListener();
        rv.addOnItemTouchListener(listener);
        rv.removeOnItemTouchListener(listener);

        ProbeResult result = ListenerProbe.probeRecyclerViewItemTouchListeners(rv);

        assertTrue(result.fieldFound);
        assertFalse("Listener was removed, should not be detected", result.valueDetected);
    }

    // ==================== RecyclerView 子类测试 ====================

    /**
     * 模拟 RecyclerView 子类，验证向上遍历 class hierarchy 能找到字段。
     */
    @Test
    public void recyclerViewSubclass_fieldFoundViaClassHierarchyTraversal() {
        RecyclerView subclassRv = new RecyclerView(context) {};
        subclassRv.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener());

        ProbeResult result = ListenerProbe.probeRecyclerViewItemTouchListeners(subclassRv);

        assertTrue("Field should be found via class hierarchy traversal", result.fieldFound);
        assertTrue("Listener should be detected", result.valueDetected);
        assertNotNull("Declaring class should be reported", result.fieldDeclaringClass);
    }

    // ==================== View.hasOnClickListeners() 测试 ====================

    @Test
    public void view_noOnClickListener_hasOnClickListenersReturnsFalse() {
        View view = new View(context);

        ProbeResult result = ListenerProbe.probeHasOnClickListeners(view);

        assertTrue("Public API should always succeed", result.fieldFound);
        assertFalse("No listener set", result.valueDetected);
    }

    @Test
    public void view_withOnClickListener_hasOnClickListenersReturnsTrue() {
        View view = new View(context);
        view.setOnClickListener(v -> {});

        ProbeResult result = ListenerProbe.probeHasOnClickListeners(view);

        assertTrue("Public API should always succeed", result.fieldFound);
        assertTrue("Listener was set, should be detected", result.valueDetected);
    }
}
