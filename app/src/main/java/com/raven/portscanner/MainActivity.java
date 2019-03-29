package com.raven.portscanner;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ScannerEndCallBackListener, RadioGroup.OnCheckedChangeListener {


    Button confirmBt;
    EditText hostEdit;
    FrameLayout frameLayout;

    RadioGroup modelChoose;

    RangeScannerFragment rangeFragment = new RangeScannerFragment();
    SetsScannerFragment setsFragment = new SetsScannerFragment();

    final String rangeScannerTag = "range";
    final String setsScannerTag = "sets";

    boolean scanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* 初始话views */
        initViews();
    }

    /**
     * 初始话Views
     */
    private void initViews() {
        confirmBt = findViewById(R.id.confirm_bt);
        hostEdit = findViewById(R.id.host_edit);
        modelChoose = findViewById(R.id.radio_group);
        frameLayout = findViewById(R.id.frame_layout);

        confirmBt.setOnClickListener(this);
        modelChoose.setOnCheckedChangeListener(this);
        /* 初始化扫描模式 */
        initScannerModel();
    }

    private void initScannerModel() {
        /* 初始话选择项*/
        initCheckBox();
        /* 加载端口范围的的fragment */
        initFragment();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.replace(R.id.frame_layout, rangeFragment);
        ft.commit();
    }

    private void initCheckBox() {
        /* 默认第一个模式，范围扫描 */
        modelChoose.check(R.id.port_range);
    }

    private void initFragment() {
        /* 加入回调等等 */
        rangeFragment.setCallBackListener(this);
        setsFragment.setCallBackListener(this);
    }

    /**
     * 更改fragment
     *
     * @param from 从哪个fragment
     * @param to   更改到哪个fragment
     */
    private void changeFragment(Fragment from, Fragment to) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.remove(from);
        ft.replace(R.id.frame_layout, to);
        ft.commit();
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.confirm_bt) {
            if (!scanning) {

                /* 判断IP正确与否 */
                String ip = hostEdit.getText().toString();
                if (!checkIp(ip)) {
                    Toast.makeText(this, "ip格式错误，请检查", Toast.LENGTH_SHORT).show();
                    scanEndCallback();
                    return;
                }
                /* 开始扫描 */
                /* 设置button 在结束扫描前不可在使用 */
                confirmBt.setTextColor(0xBDBDBD);
                confirmBt.setTextColor(Color.parseColor("#BDBDBD"));
                confirmBt.setClickable(false);
                scanning = true;
                /* 进行扫描操作 */
                if (R.id.port_range == modelChoose.getCheckedRadioButtonId()) {
                    rangeFragment.startScanner(ip);
                } else if (R.id.sets_port == modelChoose.getCheckedRadioButtonId()) {
                    setsFragment.startScan(ip);
                }
            }
        }
    }

    private boolean checkIp(String ip) {
        if (ip != null && !ip.isEmpty()) {
            // 定义正则表达式
            String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                    + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                    + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                    + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
            return ip.matches(regex);
        }
        return false;
    }

    @Override
    public void scanEndCallback() {
        /* 扫描完成，重新初始化 Activity state*/
        confirmBt.setTextColor(Color.parseColor("#FFFFFF"));
        confirmBt.setClickable(true);
        scanning = false;
        Log.i("Activity","back");
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (R.id.port_range == checkedId) {
            /* range 模式 */
            changeFragment(setsFragment, rangeFragment);
        } else if (R.id.sets_port == checkedId) {
            /* sets 模式 */
            changeFragment(rangeFragment, setsFragment);
        }
    }
}
