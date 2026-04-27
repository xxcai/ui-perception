package com.hh.uiperception.baseline.nativepage;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hh.uiperception.baseline.R;

public class NativeHomeActivity extends Activity {
    public static final String EXTRA_ROUTE = "baseline_route";

    private static final int BLUE = 0xFF1593FF;
    private static final int TEXT_PRIMARY = 0xFF2F3136;
    private static final int TEXT_SECONDARY = 0xFF8A8D91;
    private static final int DIVIDER = 0xFFEDEFF2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createMessageHome());
    }

    private View createMessageHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        root.addView(createTopArea(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(createFilterTabs(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(40)
        ));
        root.addView(createConcernBanner(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(36)
        ));
        root.addView(createConversationList(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        root.addView(createBottomTabs(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
        ));

        return root;
    }

    private View createTopArea() {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(16), dp(10), dp(16), dp(8));
        top.setBackgroundColor(Color.WHITE);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);

        titleRow.addView(iconImage(R.drawable.ic_profile, R.drawable.bg_avatar_blue), new LinearLayout.LayoutParams(dp(40), dp(40)));

        TextView title = new TextView(this);
        title.setText("消息");
        title.setTextColor(TEXT_PRIMARY);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(dp(18), 0, 0, 0);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        titleRow.addView(iconImage(R.drawable.ic_phone, 0), new LinearLayout.LayoutParams(dp(40), dp(40)));
        titleRow.addView(iconImage(R.drawable.ic_plus, 0), new LinearLayout.LayoutParams(dp(40), dp(40)));
        top.addView(titleRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setPadding(0, dp(8), 0, 0);

        TextView search = new TextView(this);
        search.setText("   搜索");
        search.setTextSize(16);
        search.setTextColor(0xFFC9CDD2);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(dp(16), 0, dp(12), 0);
        search.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0);
        search.setCompoundDrawablePadding(dp(8));
        search.setBackgroundResource(R.drawable.bg_search);
        searchRow.addView(search, new LinearLayout.LayoutParams(0, dp(36), 1f));

        LinearLayout service = new LinearLayout(this);
        service.setOrientation(LinearLayout.VERTICAL);
        service.setGravity(Gravity.CENTER);
        service.setPadding(dp(12), 0, 0, 0);
        TextView headset = new TextView(this);
        headset.setText("客服");
        headset.setTextSize(12);
        headset.setGravity(Gravity.CENTER);
        headset.setTextColor(TEXT_PRIMARY);
        TextView serviceText = new TextView(this);
        serviceText.setText("");
        serviceText.setTextSize(1);
        serviceText.setGravity(Gravity.CENTER);
        serviceText.setTextColor(TEXT_PRIMARY);
        service.addView(headset);
        service.addView(serviceText);
        searchRow.addView(service, new LinearLayout.LayoutParams(dp(64), dp(42)));

        top.addView(searchRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        return top;
    }

    private View createFilterTabs() {
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setBackgroundColor(Color.WHITE);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(8), 0);

        row.addView(filterItem(R.drawable.ic_chat, "", true));
        row.addView(filterText("@", "我"));
        row.addView(filterItem(R.drawable.ic_check_square, "稍后", false));
        row.addView(filterText("◔", "未读"));
        row.addView(filterItem(R.drawable.ic_star_outline, "特别关注", false));
        row.addView(filterItem(R.drawable.ic_menu, "", false));

        scroller.addView(row, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return scroller;
    }

    private View createConcernBanner() {
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(dp(16), 0, dp(12), 0);
        banner.setBackgroundResource(R.drawable.bg_banner);

        banner.addView(iconImage(R.drawable.ic_star_filled, 0), new LinearLayout.LayoutParams(dp(30), dp(30)));

        TextView text = new TextView(this);
        text.setText("添加特别关注，重要消息不遗漏");
        text.setTextColor(BLUE);
        text.setTextSize(17);
        banner.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = new TextView(this);
        arrow.setText(">");
        arrow.setTextColor(BLUE);
        arrow.setTextSize(24);
        arrow.setGravity(Gravity.CENTER);
        banner.addView(arrow, new LinearLayout.LayoutParams(dp(36), ViewGroup.LayoutParams.WRAP_CONTENT));

        return banner;
    }

    private View createConversationList() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setBackgroundColor(Color.WHITE);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.addView(conversation("1", "1", "早上好", "04/22", "", false, R.drawable.bg_avatar_gray, 0xFFFFFFFF));
        list.addView(conversation("辉", "彭子辉", "明天上班", "04/22", "", false, R.drawable.bg_avatar_cyan, 0xFF00A6D6));
        list.addView(conversation("文件", "文件号", "", "04/20", "", false, R.drawable.bg_avatar_purple, 0xFFFFFFFF));
        list.addView(conversation("应用", "应用号", "", "04/20", "", true, R.drawable.bg_avatar_purple, 0xFFFFFFFF));
        list.addView(conversation("云", "杨兵", "早上好", "04/02", "", false, R.drawable.bg_avatar_light, 0xFF7FA6FF));
        list.addView(conversation("组", "消息卡片测试小组-UAT", "杨兵: [卡片]testy_26040102", "04/01", "团队", false, R.drawable.bg_avatar_light, BLUE));
        list.addView(conversation("柳", "谢传柳", "明天放假", "03/31", "", false, R.drawable.bg_avatar_light, BLUE));
        list.addView(conversation("峰", "蔡峰", "晚上要加班", "03/20", "", false, R.drawable.bg_avatar_violet, 0xFF7C4DFF));
        list.addView(conversation("叶", "叶明星", "周报已提交", "03/20", "", false, R.drawable.bg_avatar_light, 0xFF527DFF));

        scrollView.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return scrollView;
    }

    private View createBottomTabs() {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        tabs.setBackgroundColor(0xFFF9FAFB);
        tabs.setPadding(0, dp(8), 0, dp(6));

        tabs.addView(bottomTextTab("●", "助理", false), weightParams());
        tabs.addView(bottomTab(R.drawable.ic_chat, "消息", true), weightParams());
        tabs.addView(bottomTab(R.drawable.ic_mail, "邮件", false), weightParams());
        tabs.addView(bottomTextTab("◎", "通讯录", false), weightParams());
        tabs.addView(bottomTab(R.drawable.ic_grid, "业务", false), weightParams());
        tabs.addView(bottomTab(R.drawable.ic_doc, "知识", false), weightParams());
        return tabs;
    }

    private View conversation(
            String avatarText,
            String name,
            String info,
            String date,
            String tag,
            boolean muted,
            int avatarBgRes,
            int avatarFg
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        row.setPadding(dp(16), dp(10), dp(16), 0);
        row.setMinimumHeight(dp(72));

        TextView avatar = iconBox(avatarText, avatarFg, avatarBgRes);
        row.addView(avatar, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), 0, 0, 0);

        LinearLayout titleLine = new LinearLayout(this);
        titleLine.setOrientation(LinearLayout.HORIZONTAL);
        titleLine.setGravity(Gravity.CENTER_VERTICAL);

        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextColor(TEXT_PRIMARY);
        nameView.setTextSize(18);
        nameView.setSingleLine(true);
        titleLine.addView(nameView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (!tag.isEmpty()) {
            TextView tagView = new TextView(this);
            tagView.setText(tag);
            tagView.setTextColor(BLUE);
            tagView.setTextSize(13);
            tagView.setPadding(dp(6), dp(1), dp(6), dp(1));
            tagView.setBackgroundResource(R.drawable.bg_tag);
            titleLine.addView(tagView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }

        TextView dateView = new TextView(this);
        dateView.setText(date);
        dateView.setTextColor(TEXT_SECONDARY);
        dateView.setTextSize(14);
        dateView.setGravity(Gravity.END);
        titleLine.addView(dateView, new LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout subtitleLine = new LinearLayout(this);
        subtitleLine.setOrientation(LinearLayout.HORIZONTAL);
        subtitleLine.setGravity(Gravity.CENTER_VERTICAL);
        subtitleLine.setPadding(0, dp(4), 0, dp(10));

        TextView infoView = new TextView(this);
        infoView.setText(info);
        infoView.setTextColor(TEXT_SECONDARY);
        infoView.setTextSize(15);
        infoView.setSingleLine(true);
        subtitleLine.addView(infoView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (muted) {
            subtitleLine.addView(iconImage(R.drawable.ic_mute, 0), new LinearLayout.LayoutParams(dp(22), dp(22)));
        }

        View divider = new View(this);
        divider.setBackgroundColor(DIVIDER);

        body.addView(titleLine);
        body.addView(subtitleLine);
        body.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
        ));
        row.addView(body, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return row;
    }

    private View filterItem(int iconRes, String text, boolean active) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(5), 0, dp(5), 0);

        item.addView(iconImage(iconRes, 0), new LinearLayout.LayoutParams(dp(24), dp(24)));

        if (!text.isEmpty()) {
            TextView textView = new TextView(this);
            textView.setText(text);
            textView.setTextSize(16);
            textView.setTextColor(0xFF676A6F);
            textView.setPadding(dp(4), 0, 0, 0);
            item.addView(textView);
        }

        return item;
    }

    private View filterText(String icon, String text) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(5), 0, dp(5), 0);

        TextView iconView = new TextView(this);
        iconView.setText(icon);
        iconView.setTextSize(16);
        iconView.setTextColor(0xFF6D7075);
        item.addView(iconView);

        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(0xFF676A6F);
        textView.setPadding(dp(4), 0, 0, 0);
        item.addView(textView);
        return item;
    }

    private View bottomTab(int iconRes, String label, boolean selected) {
        LinearLayout tab = new LinearLayout(this);
        tab.setOrientation(LinearLayout.VERTICAL);
        tab.setGravity(Gravity.CENTER);
        int color = selected ? BLUE : 0xFF5F6368;

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTextColor(color);
        labelView.setGravity(Gravity.CENTER);
        labelView.setPadding(0, dp(3), 0, 0);

        tab.addView(iconImage(iconRes, 0), new LinearLayout.LayoutParams(dp(24), dp(24)));
        tab.addView(labelView);
        return tab;
    }

    private View bottomTextTab(String icon, String label, boolean selected) {
        LinearLayout tab = new LinearLayout(this);
        tab.setOrientation(LinearLayout.VERTICAL);
        tab.setGravity(Gravity.CENTER);
        int color = selected ? BLUE : 0xFF5F6368;

        TextView iconView = new TextView(this);
        iconView.setText(icon);
        iconView.setTextSize(18);
        iconView.setTextColor(color);
        iconView.setGravity(Gravity.CENTER);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTextColor(color);
        labelView.setGravity(Gravity.CENTER);
        labelView.setPadding(0, dp(3), 0, 0);

        tab.addView(iconView, new LinearLayout.LayoutParams(dp(24), dp(24)));
        tab.addView(labelView);
        return tab;
    }

    private TextView iconBox(String text, int textColor, int bgRes) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(textColor);
        view.setTextSize(text.length() > 1 ? 12 : 18);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(bgRes);
        return view;
    }

    private ImageView iconImage(int drawableRes, int backgroundRes) {
        ImageView image = new ImageView(this);
        image.setImageResource(drawableRes);
        if (backgroundRes != 0) {
            image.setBackgroundResource(backgroundRes);
            image.setPadding(dp(8), dp(8), dp(8), dp(8));
        }
        image.setScaleType(ImageView.ScaleType.CENTER);
        return image;
    }

    private LinearLayout.LayoutParams weightParams() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

}
