package com.actia.monitor.actia_monitor.test;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import com.actia.monitor.actia_monitor.R;
import com.actia.monitor.actia_monitor.view.DashboardView;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * 测试效果
 * author:zhenyu chen
 * date:2017/5/31.
 */
public class TestActivity extends Activity {

    @InjectView(R.id.dv)
    DashboardView dv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        ButterKnife.inject(this);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //dv.smoothDotMove(180);
                dv.smoothDotMove(270);
            }
        }, 1000);
    }
}
