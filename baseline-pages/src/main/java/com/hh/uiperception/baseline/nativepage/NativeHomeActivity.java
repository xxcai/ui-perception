package com.hh.uiperception.baseline.nativepage;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hh.uiperception.baseline.R;
import com.hh.uiperception.baseline.BaselineRoutes;

public class NativeHomeActivity extends Activity {
    public static final String EXTRA_ROUTE = "baseline_route";

    private static final int TAB_ASSISTANT = 0;
    private static final int TAB_MESSAGE = 1;
    private static final int TAB_MAIL = 2;
    private static final int TAB_CONTACTS = 3;
    private static final int TAB_WORK = 4;
    private static final int TAB_KNOWLEDGE = 5;

    private LinearLayout bottomTabs;
    private int contentContainerId;
    private int selectedTab = TAB_MESSAGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contentContainerId = View.generateViewId();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        android.widget.FrameLayout content = new android.widget.FrameLayout(this);
        content.setId(contentContainerId);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        bottomTabs = new LinearLayout(this);
        bottomTabs.setOrientation(LinearLayout.HORIZONTAL);
        bottomTabs.setGravity(Gravity.CENTER);
        bottomTabs.setBackgroundColor(0xFFF9FAFB);
        bottomTabs.setPadding(0, dp(8), 0, dp(6));
        root.addView(bottomTabs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
        ));

        setContentView(root);
        String route = getIntent().getStringExtra(EXTRA_ROUTE);
        if (BaselineRoutes.NATIVE_HOME_MAIL.equals(route)) {
            selectTab(TAB_MAIL);
        } else if (BaselineRoutes.NATIVE_HOME_CONTACTS.equals(route)) {
            selectTab(TAB_CONTACTS);
        } else {
            selectTab(TAB_MESSAGE);
        }
    }

    private void selectTab(int tab) {
        selectedTab = tab;
        Fragment fragment;
        if (tab == TAB_MAIL) {
            fragment = new MailHomeFragment();
        } else if (tab == TAB_CONTACTS) {
            fragment = new ContactsHomeFragment();
        } else {
            fragment = new MessageHomeFragment();
            selectedTab = TAB_MESSAGE;
        }

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(contentContainerId, fragment);
        transaction.commit();
        renderBottomTabs();
    }

    private void renderBottomTabs() {
        bottomTabs.removeAllViews();
        bottomTabs.addView(bottomTab(R.drawable.ic_assistant, "助理", selectedTab == TAB_ASSISTANT, null), weightParams());
        bottomTabs.addView(bottomTab(R.drawable.ic_chat, "消息", selectedTab == TAB_MESSAGE, () -> selectTab(TAB_MESSAGE)), weightParams());
        bottomTabs.addView(bottomTab(R.drawable.ic_mail, "邮件", selectedTab == TAB_MAIL, () -> selectTab(TAB_MAIL)), weightParams());
        bottomTabs.addView(bottomTab(R.drawable.ic_contacts, "通讯录", selectedTab == TAB_CONTACTS, () -> selectTab(TAB_CONTACTS)), weightParams());
        bottomTabs.addView(bottomTab(R.drawable.ic_grid, "业务", selectedTab == TAB_WORK, null), weightParams());
        bottomTabs.addView(bottomTab(R.drawable.ic_doc, "知识", selectedTab == TAB_KNOWLEDGE, null), weightParams());
    }

    void openMailTab() {
        selectTab(TAB_MAIL);
    }

    void openContactsTab() {
        selectTab(TAB_CONTACTS);
    }

    private View bottomTab(int iconRes, String label, boolean selected, Runnable action) {
        LinearLayout tab = new LinearLayout(this);
        tab.setOrientation(LinearLayout.VERTICAL);
        tab.setGravity(Gravity.CENTER);
        tab.setClickable(action != null);
        if (action != null) {
            tab.setOnClickListener(v -> action.run());
        }

        int color = selected ? UiKit.BLUE : 0xFF5F6368;
        ImageView icon = UiKit.iconImage(this, iconRes, 0);
        icon.setColorFilter(color);
        tab.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTextColor(color);
        labelView.setGravity(Gravity.CENTER);
        labelView.setPadding(0, dp(3), 0, 0);
        tab.addView(labelView);

        return tab;
    }

    private LinearLayout.LayoutParams weightParams() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
    }

    private int dp(int value) {
        return UiKit.dp(this, value);
    }
}
