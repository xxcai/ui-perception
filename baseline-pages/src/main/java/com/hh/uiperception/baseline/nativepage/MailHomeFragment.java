package com.hh.uiperception.baseline.nativepage;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hh.uiperception.baseline.R;

import java.util.ArrayList;
import java.util.List;

public class MailHomeFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(getActivity());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        root.addView(createTopArea(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setBackgroundColor(Color.WHITE);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(new MailAdapter(createMailItems()));
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(recyclerView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        return root;
    }

    private View createTopArea() {
        LinearLayout top = new LinearLayout(getActivity());
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(16), dp(10), dp(16), 0);
        top.setBackgroundColor(Color.WHITE);

        LinearLayout titleRow = new LinearLayout(getActivity());
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);

        titleRow.addView(UiKit.iconImage(getActivity(), R.drawable.ic_profile, R.drawable.bg_avatar_blue),
                new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout titleGroup = new LinearLayout(getActivity());
        titleGroup.setOrientation(LinearLayout.HORIZONTAL);
        titleGroup.setGravity(Gravity.CENTER_VERTICAL);
        titleGroup.setPadding(dp(18), 0, 0, 0);

        TextView title = UiKit.title(getActivity(), "邮件");
        titleGroup.addView(title);
        ImageView arrow = UiKit.iconImage(getActivity(), R.drawable.ic_chevron_down, 0);
        arrow.setColorFilter(UiKit.TEXT_PRIMARY);
        titleGroup.addView(arrow, new LinearLayout.LayoutParams(dp(24), dp(24)));
        titleRow.addView(titleGroup, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView filter = UiKit.iconImage(getActivity(), R.drawable.ic_toggle_filter, 0);
        filter.setAlpha(0.35f);
        titleRow.addView(filter, new LinearLayout.LayoutParams(dp(40), dp(40)));

        titleRow.addView(UiKit.iconImage(getActivity(), R.drawable.ic_plus, 0), new LinearLayout.LayoutParams(dp(40), dp(40)));
        top.addView(titleRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));

        top.addView(createSearchRow(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
        ));

        View divider = new View(getActivity());
        divider.setBackgroundColor(UiKit.DIVIDER);
        top.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
        ));
        return top;
    }

    private View createSearchRow() {
        LinearLayout row = new LinearLayout(getActivity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView search = new TextView(getActivity());
        search.setText("搜索");
        search.setTextSize(16);
        search.setTextColor(0xFFC9CDD2);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(dp(16), 0, dp(12), 0);
        search.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0);
        search.setCompoundDrawablePadding(dp(8));
        search.setBackgroundResource(R.drawable.bg_search);
        row.addView(search, new LinearLayout.LayoutParams(0, dp(36), 1f));

        LinearLayout service = new LinearLayout(getActivity());
        service.setOrientation(LinearLayout.VERTICAL);
        service.setGravity(Gravity.CENTER);
        service.setPadding(dp(12), 0, 0, 0);
        ImageView icon = UiKit.iconImage(getActivity(), R.drawable.ic_headset, 0);
        service.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));
        TextView label = new TextView(getActivity());
        label.setText("客服");
        label.setTextSize(12);
        label.setTextColor(UiKit.TEXT_PRIMARY);
        label.setGravity(Gravity.CENTER);
        service.addView(label);
        row.addView(service, new LinearLayout.LayoutParams(dp(64), dp(42)));
        return row;
    }

    private List<MailItem> createMailItems() {
        List<MailItem> items = new ArrayList<>();
        items.add(new MailItem("平台通知", "测试计划同步", "请确认本周 UI 感知测试计划，补充邮件页样例后统一回归。", "09:12"));
        items.add(new MailItem("产品运营", "首页改版体验反馈", "邮件入口已切换到新导航，请关注空态和列表态在不同账号下的表现。", "昨天"));
        items.add(new MailItem("质量保障部", "自动化巡检日报", "今日巡检共覆盖 18 个页面，邮件列表识别项等待新增基线。", "周一"));
        items.add(new MailItem("项目管理办公室", "四月版本节奏确认", "请各模块负责人同步当前风险、阻塞和需要协同的事项。", "04/25"));
        items.add(new MailItem("系统提醒", "会议纪要已生成", "你参与的 UI 感知方案讨论已生成纪要，可在知识库中查看。", "04/24"));
        items.add(new MailItem("研发效能平台", "构建结果通知", "baseline-pages 模块最新构建任务已完成，请检查产物状态。", "04/22"));
        items.add(new MailItem("安全合规", "测试数据使用提醒", "请勿在基准页面中保留真实用户、客户或生产业务数据。", "04/20"));
        items.add(new MailItem("体验设计", "邮件列表视觉规范", "列表项需保持发送人、主题、摘要、时间的稳定层级。", "04/18"));
        return items;
    }

    private int dp(int value) {
        return UiKit.dp(getActivity(), value);
    }
}
