package com.hh.uiperception.baseline.nativepage;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hh.uiperception.baseline.R;

import java.util.List;

final class BusinessAdapter extends BaseAdapter {
    private final List<BusinessRow> rows;

    BusinessAdapter(List<BusinessRow> rows) {
        this.rows = rows;
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public BusinessRow getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 11;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BusinessRow row = rows.get(position);
        switch (row.type) {
            case BusinessRow.TYPE_SEARCH:
                return searchRow(parent);
            case BusinessRow.TYPE_RECENT:
                return recentRow(parent, row);
            case BusinessRow.TYPE_EMPTY:
                return emptyCard(parent, row);
            case BusinessRow.TYPE_BANNER:
                return bannerCard(parent, row);
            case BusinessRow.TYPE_ATTENDANCE:
                return attendanceCard(parent, row);
            case BusinessRow.TYPE_APPROVAL:
                return approvalCard(parent, row);
            case BusinessRow.TYPE_PLINK:
                return plinkCard(parent, row);
            case BusinessRow.TYPE_NOTICE:
                return noticeCard(parent, row);
            case BusinessRow.TYPE_IMAGE_BULLET:
                return imageBulletCard(parent, row);
            case BusinessRow.TYPE_APP_GRID:
                return appGrid(parent, row);
            default:
                return simpleRow(parent, row);
        }
    }

    private View searchRow(ViewGroup parent) {
        LinearLayout row = new LinearLayout(parent.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(parent, 16), dp(parent, 8), dp(parent, 16), dp(parent, 8));
        row.setBackgroundColor(Color.WHITE);

        TextView search = new TextView(parent.getContext());
        search.setText("搜索");
        search.setTextSize(16);
        search.setTextColor(0xFFC9CDD2);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(dp(parent, 16), 0, dp(parent, 12), 0);
        search.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0);
        search.setCompoundDrawablePadding(dp(parent, 8));
        search.setBackgroundResource(R.drawable.bg_search);
        UiKit.markClickable(search);
        row.addView(search, new LinearLayout.LayoutParams(0, dp(parent, 36), 1f));

        LinearLayout service = new LinearLayout(parent.getContext());
        service.setOrientation(LinearLayout.VERTICAL);
        service.setGravity(Gravity.CENTER);
        service.setPadding(dp(parent, 12), 0, 0, 0);
        UiKit.markClickable(service);
        service.addView(UiKit.iconImage(parent.getContext(), R.drawable.ic_headset, 0),
                new LinearLayout.LayoutParams(dp(parent, 22), dp(parent, 22)));
        TextView label = text(parent, "客服", UiKit.TEXT_PRIMARY, 12);
        label.setGravity(Gravity.CENTER);
        service.addView(label);
        row.addView(service, new LinearLayout.LayoutParams(dp(parent, 64), dp(parent, 42)));
        return row;
    }

    private View recentRow(ViewGroup parent, BusinessRow row) {
        LinearLayout card = card(parent);
        card.addView(titleLine(parent, row.title, ""));

        LinearLayout apps = new LinearLayout(parent.getContext());
        apps.setOrientation(LinearLayout.HORIZONTAL);
        apps.setPadding(dp(parent, 8), dp(parent, 10), 0, dp(parent, 12));
        apps.addView(appIcon(parent, R.drawable.ic_business_wifi, "华为Wi-Fi"));
        apps.addView(appIcon(parent, R.drawable.ic_business_device, "设备管理"));
        card.addView(apps);

        TextView mall = text(parent, row.subtitle, UiKit.TEXT_SECONDARY, 16);
        mall.setPadding(dp(parent, 12), dp(parent, 8), dp(parent, 12), dp(parent, 12));
        card.addView(mall);
        return withOuterMargin(parent, card);
    }

    private View simpleRow(ViewGroup parent, BusinessRow row) {
        LinearLayout card = card(parent);
        card.addView(titleLine(parent, row.title, row.rightText));
        return withOuterMargin(parent, card);
    }

    private View emptyCard(ViewGroup parent, BusinessRow row) {
        LinearLayout card = card(parent);
        card.addView(titleLine(parent, row.title, ""));
        TextView message = text(parent, row.subtitle, 0xFFC9CDD2, 17);
        message.setGravity(Gravity.CENTER);
        card.addView(message, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(parent, 78)
        ));
        return withOuterMargin(parent, card);
    }

    private View bannerCard(ViewGroup parent, BusinessRow row) {
        LinearLayout card = card(parent);
        card.addView(titleLine(parent, row.title, ""));

        ImageView banner = new ImageView(parent.getContext());
        banner.setImageResource(row.iconRes);
        banner.setScaleType(ImageView.ScaleType.FIT_XY);
        card.addView(banner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(parent, 150)
        ));
        return withOuterMargin(parent, card);
    }

    private View attendanceCard(ViewGroup parent, BusinessRow row) {
        LinearLayout card = card(parent);
        card.addView(titleLine(parent, row.title, ""));

        LinearLayout body = new LinearLayout(parent.getContext());
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setGravity(Gravity.CENTER_VERTICAL);
        body.setPadding(dp(parent, 20), dp(parent, 20), dp(parent, 28), dp(parent, 22));

        TextView hint = text(parent, row.subtitle, 0xFFC9CDD2, 16);
        body.addView(hint, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        FrameLayout action = new FrameLayout(parent.getContext());
        UiKit.markClickable(action);
        ImageView circle = new ImageView(parent.getContext());
        circle.setImageResource(R.drawable.bg_business_punch);
        circle.setScaleType(ImageView.ScaleType.FIT_XY);
        action.addView(circle, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        TextView buttonText = text(parent, "打卡", Color.WHITE, 22);
        buttonText.setGravity(Gravity.CENTER);
        buttonText.setTypeface(Typeface.DEFAULT_BOLD);
        action.addView(buttonText, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        body.addView(action, new LinearLayout.LayoutParams(dp(parent, 116), dp(parent, 116)));
        card.addView(body);
        return withOuterMargin(parent, card);
    }

    private View approvalCard(ViewGroup parent, BusinessRow row) {
        LinearLayout card = card(parent);
        card.addView(titleLine(parent, row.title, ""));
        for (String item : row.items) {
            String[] parts = item.split("\\|", 2);
            LinearLayout line = new LinearLayout(parent.getContext());
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(dp(parent, 20), dp(parent, 9), dp(parent, 20), dp(parent, 9));
            UiKit.markClickable(line);
            TextView name = text(parent, parts[0], UiKit.TEXT_PRIMARY, 17);
            line.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            String count = parts.length > 1 ? parts[1] : "";
            TextView badge = text(parent, count, Color.WHITE, 12);
            badge.setGravity(Gravity.CENTER);
            badge.setBackgroundResource(R.drawable.bg_business_badge);
            line.addView(badge, new LinearLayout.LayoutParams(dp(parent, 34), dp(parent, 34)));
            card.addView(line);
        }
        return withOuterMargin(parent, card);
    }

    private View plinkCard(ViewGroup parent, BusinessRow row) {
        LinearLayout card = card(parent);
        card.addView(titleLine(parent, row.title, ""));
        TextView empty = text(parent, row.subtitle, 0xFFC9CDD2, 17);
        empty.setGravity(Gravity.CENTER);
        card.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(parent, 76)));

        LinearLayout tabs = new LinearLayout(parent.getContext());
        tabs.setGravity(Gravity.CENTER);
        tabs.setPadding(dp(parent, 16), 0, dp(parent, 16), dp(parent, 12));
        for (String item : row.items) {
            TextView tab = text(parent, item, UiKit.BLUE, 16);
            tab.setGravity(Gravity.CENTER);
            UiKit.markClickable(tab);
            tabs.addView(tab, new LinearLayout.LayoutParams(0, dp(parent, 40), 1f));
        }
        card.addView(tabs);
        return withOuterMargin(parent, card);
    }

    private View noticeCard(ViewGroup parent, BusinessRow row) {
        LinearLayout card = card(parent);
        card.addView(titleLine(parent, row.title, ""));
        LinearLayout body = new LinearLayout(parent.getContext());
        body.setGravity(Gravity.CENTER_VERTICAL);
        body.setPadding(dp(parent, 20), dp(parent, 8), dp(parent, 20), dp(parent, 16));
        UiKit.markClickable(body);
        body.addView(UiKit.iconImage(parent.getContext(), R.drawable.ic_business_notice, 0),
                new LinearLayout.LayoutParams(dp(parent, 34), dp(parent, 34)));
        TextView message = text(parent, row.subtitle, UiKit.TEXT_PRIMARY, 17);
        message.setSingleLine(true);
        message.setPadding(dp(parent, 12), 0, 0, 0);
        body.addView(message, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(body);
        return withOuterMargin(parent, card);
    }

    private View imageBulletCard(ViewGroup parent, BusinessRow row) {
        LinearLayout card = card(parent);
        card.addView(titleLine(parent, row.title, ""));

        LinearLayout top = new LinearLayout(parent.getContext());
        top.setPadding(dp(parent, 20), dp(parent, 8), dp(parent, 20), dp(parent, 14));
        top.setGravity(Gravity.CENTER_VERTICAL);
        UiKit.markClickable(top);
        top.addView(UiKit.iconImage(parent.getContext(), R.drawable.ic_business_report, 0),
                new LinearLayout.LayoutParams(dp(parent, 108), dp(parent, 82)));
        LinearLayout textGroup = new LinearLayout(parent.getContext());
        textGroup.setOrientation(LinearLayout.VERTICAL);
        textGroup.setPadding(dp(parent, 16), 0, 0, 0);
        textGroup.addView(text(parent, row.items.get(0), UiKit.TEXT_PRIMARY, 18));
        TextView subtitle = text(parent, row.subtitle, UiKit.TEXT_SECONDARY, 15);
        subtitle.setPadding(0, dp(parent, 6), 0, 0);
        textGroup.addView(subtitle);
        top.addView(textGroup, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(top);

        for (int i = 1; i < row.items.size(); i++) {
            LinearLayout line = new LinearLayout(parent.getContext());
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(dp(parent, 28), dp(parent, 7), dp(parent, 20), dp(parent, 7));
            UiKit.markClickable(line);
            View dot = new View(parent.getContext());
            dot.setBackgroundResource(R.drawable.bg_business_bullet_dot);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(parent, 5), dp(parent, 5));
            dotParams.setMargins(0, 0, dp(parent, 10), 0);
            line.addView(dot, dotParams);
            TextView bullet = text(parent, row.items.get(i), UiKit.TEXT_PRIMARY, 17);
            line.addView(bullet, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            card.addView(line);
        }
        return withOuterMargin(parent, card);
    }

    private View appGrid(ViewGroup parent, BusinessRow row) {
        LinearLayout card = card(parent);
        TextView title = text(parent, row.title, UiKit.TEXT_PRIMARY, 18);
        title.setPadding(dp(parent, 20), dp(parent, 18), dp(parent, 20), dp(parent, 10));
        card.addView(title);

        GridLayout grid = new GridLayout(parent.getContext());
        grid.setColumnCount(5);
        grid.setPadding(dp(parent, 8), 0, dp(parent, 8), dp(parent, 18));
        int[] icons = {
                R.drawable.ic_business_video,
                R.drawable.ic_business_topic,
                R.drawable.ic_business_square,
                R.drawable.ic_business_customer,
                R.drawable.ic_business_hotline,
                R.drawable.ic_business_more
        };
        for (int i = 0; i < row.items.size(); i++) {
            grid.addView(gridApp(parent, icons[i], row.items.get(i)));
        }
        card.addView(grid);
        return withOuterMargin(parent, card);
    }

    private View appIcon(ViewGroup parent, int iconRes, String label) {
        LinearLayout item = new LinearLayout(parent.getContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(parent, 4), 0, dp(parent, 4), 0);
        UiKit.markClickable(item);
        item.addView(UiKit.iconImage(parent.getContext(), iconRes, 0),
                new LinearLayout.LayoutParams(dp(parent, 52), dp(parent, 52)));
        TextView text = text(parent, label, UiKit.TEXT_PRIMARY, 14);
        text.setSingleLine(true);
        text.setGravity(Gravity.CENTER);
        text.setPadding(0, dp(parent, 8), 0, 0);
        item.addView(text);
        item.setLayoutParams(new LinearLayout.LayoutParams(dp(parent, 100), ViewGroup.LayoutParams.WRAP_CONTENT));
        return item;
    }

    private View gridApp(ViewGroup parent, int iconRes, String label) {
        LinearLayout item = new LinearLayout(parent.getContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(parent, 4), dp(parent, 8), dp(parent, 4), dp(parent, 8));
        UiKit.markClickable(item);
        item.addView(UiKit.iconImage(parent.getContext(), iconRes, 0),
                new LinearLayout.LayoutParams(dp(parent, 52), dp(parent, 52)));
        TextView text = text(parent, label, UiKit.TEXT_PRIMARY, 14);
        text.setGravity(Gravity.CENTER);
        text.setSingleLine(true);
        text.setPadding(0, dp(parent, 8), 0, 0);
        item.addView(text);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dp(parent, 98);
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        item.setLayoutParams(params);
        return item;
    }

    private LinearLayout titleLine(ViewGroup parent, String title, String right) {
        LinearLayout row = new LinearLayout(parent.getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(parent, 20), dp(parent, 14), dp(parent, 20), dp(parent, 8));
        UiKit.markClickable(row);
        TextView titleView = text(parent, title, UiKit.TEXT_PRIMARY, 18);
        titleView.setSingleLine(true);
        row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (!right.isEmpty()) {
            TextView rightView = text(parent, right, UiKit.TEXT_SECONDARY, 15);
            row.addView(rightView);
        } else {
            ImageView arrow = UiKit.iconImage(parent.getContext(), R.drawable.ic_chevron_right, 0);
            arrow.setColorFilter(0xFF9AA0A6);
            row.addView(arrow, new LinearLayout.LayoutParams(dp(parent, 20), dp(parent, 20)));
        }
        return row;
    }

    private LinearLayout card(ViewGroup parent) {
        LinearLayout card = new LinearLayout(parent.getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        return card;
    }

    private View withOuterMargin(ViewGroup parent, View child) {
        LinearLayout wrapper = new LinearLayout(parent.getContext());
        wrapper.setPadding(dp(parent, 8), dp(parent, 5), dp(parent, 8), dp(parent, 5));
        wrapper.addView(child, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return wrapper;
    }

    private TextView text(ViewGroup parent, String value, int color, int sizeSp) {
        TextView view = new TextView(parent.getContext());
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(sizeSp);
        return view;
    }

    private int dp(ViewGroup parent, int value) {
        return UiKit.dp(parent.getContext(), value);
    }
}
