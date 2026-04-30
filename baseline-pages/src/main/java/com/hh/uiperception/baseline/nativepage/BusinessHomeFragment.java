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
import android.widget.ListView;
import android.widget.TextView;

import com.hh.uiperception.baseline.R;

import java.util.ArrayList;
import java.util.List;

public class BusinessHomeFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(getActivity());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF6F7F9);

        root.addView(createTopArea(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ListView listView = new ListView(getActivity());
        listView.setDivider(null);
        listView.setSelector(android.R.color.transparent);
        listView.setCacheColorHint(Color.TRANSPARENT);
        listView.setAdapter(new BusinessAdapter(createRows()));
        listView.setOnItemClickListener((parent, view, position, id) -> {
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> true);
        root.addView(listView, new LinearLayout.LayoutParams(
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

        ImageView profile = UiKit.iconImage(getActivity(), R.drawable.ic_profile, R.drawable.bg_avatar_blue);
        UiKit.markClickable(profile);
        titleRow.addView(profile, new LinearLayout.LayoutParams(dp(40), dp(40)));

        TextView title = UiKit.title(getActivity(), "业务");
        title.setPadding(dp(18), 0, 0, 0);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView mall = UiKit.iconImage(getActivity(), R.drawable.ic_his_mall, 0);
        UiKit.markClickable(mall);
        titleRow.addView(mall, new LinearLayout.LayoutParams(dp(40), dp(40)));

        ImageView plus = UiKit.iconImage(getActivity(), R.drawable.ic_plus, 0);
        UiKit.markClickable(plus);
        titleRow.addView(plus, new LinearLayout.LayoutParams(dp(40), dp(40)));
        top.addView(titleRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));
        return top;
    }

    private List<BusinessRow> createRows() {
        List<BusinessRow> rows = new ArrayList<>();
        rows.add(BusinessRow.search());
        rows.add(BusinessRow.recent());
        rows.add(BusinessRow.empty("一般资讯高级版1", "服务不可用(10308)"));
        rows.add(BusinessRow.banner("决策在线", R.drawable.ic_business_decision_banner));
        rows.add(BusinessRow.banner("Top问题直通车", R.drawable.ic_business_top_issue_banner));
        rows.add(BusinessRow.simple("日历", "暂无内容"));
        rows.add(BusinessRow.attendance());
        rows.add(BusinessRow.approval());
        rows.add(BusinessRow.simple("iSales-经营指标", ""));
        rows.add(BusinessRow.plink());
        rows.add(BusinessRow.notice("移动申请-welink", "测试移动申请"));
        rows.add(BusinessRow.notice("融合-H5实例", "融合-H5实例（中文内容）混分巨兽九分裤付款了疯狂"));
        rows.add(BusinessRow.imageBullet());
        rows.add(BusinessRow.appGrid());
        return rows;
    }

    private int dp(int value) {
        return UiKit.dp(getActivity(), value);
    }
}
