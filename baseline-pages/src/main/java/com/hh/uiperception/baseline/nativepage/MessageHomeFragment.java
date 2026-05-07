package com.hh.uiperception.baseline.nativepage;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hh.uiperception.baseline.R;

public class MessageHomeFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(getActivity());
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
        return root;
    }

    private View createTopArea() {
        LinearLayout top = new LinearLayout(getActivity());
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(16), dp(10), dp(16), dp(8));
        top.setBackgroundColor(Color.WHITE);

        LinearLayout titleRow = new LinearLayout(getActivity());
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);

        ImageView profile = UiKit.iconImage(getActivity(), R.drawable.ic_profile, R.drawable.bg_avatar_blue);
        UiKit.markClickable(profile);
        titleRow.addView(profile, new LinearLayout.LayoutParams(dp(40), dp(40)));

        TextView title = UiKit.title(getActivity(), "消息");
        title.setPadding(dp(18), 0, 0, 0);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView phone = UiKit.iconImage(getActivity(), R.drawable.ic_phone, 0);
        UiKit.markClickable(phone);
        titleRow.addView(phone, new LinearLayout.LayoutParams(dp(40), dp(40)));

        ImageView plus = UiKit.iconImage(getActivity(), R.drawable.ic_plus, 0);
        UiKit.markClickable(plus);
        titleRow.addView(plus, new LinearLayout.LayoutParams(dp(40), dp(40)));
        top.addView(titleRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));

        top.addView(createSearchRow(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return top;
    }

    private View createSearchRow() {
        LinearLayout searchRow = new LinearLayout(getActivity());
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setPadding(0, dp(8), 0, 0);

        TextView search = new TextView(getActivity());
        search.setText("搜索");
        search.setTextSize(16);
        search.setTextColor(0xFFC9CDD2);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(dp(16), 0, dp(12), 0);
        search.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0);
        search.setCompoundDrawablePadding(dp(8));
        search.setBackgroundResource(R.drawable.bg_search);
        UiKit.markClickable(search);
        searchRow.addView(search, new LinearLayout.LayoutParams(0, dp(36), 1f));

        searchRow.addView(serviceEntry(), new LinearLayout.LayoutParams(dp(64), dp(42)));
        return searchRow;
    }

    private View serviceEntry() {
        LinearLayout service = new LinearLayout(getActivity());
        service.setOrientation(LinearLayout.VERTICAL);
        service.setGravity(Gravity.CENTER);
        service.setPadding(dp(12), 0, 0, 0);
        UiKit.markClickable(service);

        ImageView icon = UiKit.iconImage(getActivity(), R.drawable.ic_headset, 0);
        service.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView text = new TextView(getActivity());
        text.setText("客服");
        text.setTextSize(12);
        text.setGravity(Gravity.CENTER);
        text.setTextColor(UiKit.TEXT_PRIMARY);
        service.addView(text);
        return service;
    }

    private View createFilterTabs() {
        HorizontalScrollView scroller = new HorizontalScrollView(getActivity());
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setBackgroundColor(Color.WHITE);

        LinearLayout row = new LinearLayout(getActivity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(8), 0);

        row.addView(filterItem(R.drawable.ic_chat, "", true));
        row.addView(filterItem(R.drawable.ic_at_sign, "我", false));
        row.addView(filterItem(R.drawable.ic_check_square, "稍后", false));
        row.addView(filterItem(R.drawable.ic_unread, "未读", false));
        row.addView(filterItem(R.drawable.ic_star_outline, "特别关注", false));
        row.addView(filterItem(R.drawable.ic_menu, "", false));

        scroller.addView(row, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return scroller;
    }

    private View createConcernBanner() {
        LinearLayout banner = new LinearLayout(getActivity());
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(dp(16), 0, dp(12), 0);
        banner.setBackgroundResource(R.drawable.bg_banner);
        UiKit.markClickable(banner);

        banner.addView(UiKit.iconImage(getActivity(), R.drawable.ic_star_filled, 0), new LinearLayout.LayoutParams(dp(30), dp(30)));

        TextView text = new TextView(getActivity());
        text.setText("添加特别关注，重要消息不遗漏");
        text.setTextColor(UiKit.BLUE);
        text.setTextSize(17);
        banner.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView arrow = UiKit.iconImage(getActivity(), R.drawable.ic_chevron_right, 0);
        arrow.setColorFilter(UiKit.BLUE);
        banner.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(28)));
        return banner;
    }

    private View createConversationList() {
        ScrollView scrollView = new ScrollView(getActivity());
        scrollView.setFillViewport(false);
        scrollView.setBackgroundColor(Color.WHITE);

        LinearLayout list = new LinearLayout(getActivity());
        list.setOrientation(LinearLayout.VERTICAL);
        list.addView(conversation("1", "1", "早上好", "04/22", "", false, R.drawable.bg_avatar_gray, 0xFFFFFFFF));
        list.addView(conversation("梁", "梁晓舟", "明天上班", "04/22", "", false, R.drawable.bg_avatar_cyan, 0xFF00A6D6));
        list.addView(conversation("文件", "文件号", "", "04/20", "", false, R.drawable.bg_avatar_purple, 0xFFFFFFFF));
        list.addView(conversation("应用", "应用号", "", "04/20", "", true, R.drawable.bg_avatar_purple, 0xFFFFFFFF));
        list.addView(conversation("姚", "姚明远", "早上好", "04/02", "", false, R.drawable.bg_avatar_light, 0xFF7FA6FF));
        list.addView(conversation("组", "消息卡片测试小组-UAT", "姚明远: [卡片]testy_26040102", "04/01", "团队", false, R.drawable.bg_avatar_light, UiKit.BLUE));
        list.addView(conversation("夏", "夏若辰", "明天放假", "03/31", "", false, R.drawable.bg_avatar_light, UiKit.BLUE));
        list.addView(conversation("文", "文景行", "晚上要加班", "03/20", "", false, R.drawable.bg_avatar_violet, 0xFF7C4DFF));
        list.addView(conversation("岳", "岳一鸣", "周报已提交", "03/20", "", false, R.drawable.bg_avatar_light, 0xFF527DFF));

        scrollView.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return scrollView;
    }

    private View conversation(String avatarText, String name, String info, String date, String tag,
                              boolean muted, int avatarBgRes, int avatarFg) {
        LinearLayout row = new LinearLayout(getActivity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        row.setPadding(dp(16), dp(10), dp(16), 0);
        row.setMinimumHeight(dp(72));
        UiKit.markClickable(row);
        row.setOnLongClickListener(v -> true);

        TextView avatar = iconBox(avatarText, avatarFg, avatarBgRes);
        row.addView(avatar, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout body = new LinearLayout(getActivity());
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), 0, 0, 0);

        LinearLayout titleLine = new LinearLayout(getActivity());
        titleLine.setOrientation(LinearLayout.HORIZONTAL);
        titleLine.setGravity(Gravity.CENTER_VERTICAL);

        TextView nameView = new TextView(getActivity());
        nameView.setText(name);
        nameView.setTextColor(UiKit.TEXT_PRIMARY);
        nameView.setTextSize(18);
        nameView.setSingleLine(true);
        titleLine.addView(nameView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (!tag.isEmpty()) {
            TextView tagView = new TextView(getActivity());
            tagView.setText(tag);
            tagView.setTextColor(UiKit.BLUE);
            tagView.setTextSize(13);
            tagView.setPadding(dp(6), dp(1), dp(6), dp(1));
            tagView.setBackgroundResource(R.drawable.bg_tag);
            titleLine.addView(tagView);
        }

        TextView dateView = new TextView(getActivity());
        dateView.setText(date);
        dateView.setTextColor(UiKit.TEXT_SECONDARY);
        dateView.setTextSize(14);
        dateView.setGravity(Gravity.END);
        titleLine.addView(dateView, new LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout subtitleLine = new LinearLayout(getActivity());
        subtitleLine.setOrientation(LinearLayout.HORIZONTAL);
        subtitleLine.setGravity(Gravity.CENTER_VERTICAL);
        subtitleLine.setPadding(0, dp(4), 0, dp(10));

        TextView infoView = new TextView(getActivity());
        infoView.setText(info);
        infoView.setTextColor(UiKit.TEXT_SECONDARY);
        infoView.setTextSize(15);
        infoView.setSingleLine(true);
        subtitleLine.addView(infoView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (muted) {
            subtitleLine.addView(UiKit.iconImage(getActivity(), R.drawable.ic_mute, 0), new LinearLayout.LayoutParams(dp(22), dp(22)));
        }

        View divider = new View(getActivity());
        divider.setBackgroundColor(UiKit.DIVIDER);

        body.addView(titleLine);
        body.addView(subtitleLine);
        body.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        row.addView(body, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private View filterItem(int iconRes, String text, boolean active) {
        LinearLayout item = new LinearLayout(getActivity());
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(5), 0, dp(5), 0);
        UiKit.markClickable(item);

        ImageView icon = UiKit.iconImage(getActivity(), iconRes, 0);
        icon.setColorFilter(active ? UiKit.BLUE : 0xFF676A6F);
        item.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        if (!text.isEmpty()) {
            TextView textView = new TextView(getActivity());
            textView.setText(text);
            textView.setTextSize(16);
            textView.setTextColor(0xFF676A6F);
            textView.setPadding(dp(4), 0, 0, 0);
            item.addView(textView);
        }
        return item;
    }

    private TextView iconBox(String text, int textColor, int bgRes) {
        TextView view = new TextView(getActivity());
        view.setText(text);
        view.setTextColor(textColor);
        view.setTextSize(text.length() > 1 ? 12 : 18);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(bgRes);
        return view;
    }

    private int dp(int value) {
        return UiKit.dp(getActivity(), value);
    }

}
