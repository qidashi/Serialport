package com.yuncai.serialport;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;

import android_serialport_api.SerialPortFinder;


public class MainActivity extends AppCompatActivity {
    private Button button;
    private TextView tv;
    private SerialPortUtil serialPortUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
        initListener();
    }
    private void initView() {
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.btn);
        tv = findViewById(R.id.tv);
    }
    private void initData() {
        EventBus.getDefault().register(this);
        // 1.读取/proc/tty/drivers文件内容
        String[] allDevices = new SerialPortFinder().getAllDevices();
        LogUtils.d(Arrays.toString(allDevices));
        // 2.打开串口
        String portPath = "/dev/ttyS3";// 设置打开的串口
        int baudRate = 115200;// 设置波特率
        serialPortUtil = SerialPortUtil.getInstance();
        serialPortUtil.openSerialPort(portPath,baudRate);

    }
    private void initListener() {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 3.发送指令
                serialPortUtil.sendSerialPort(Cmd.OPEN_DOOR);
            }
        });
    }

    @SuppressWarnings("all")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(String string){
        LogUtils.d("来自串口的数据："+string);
        tv.setText(string);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        serialPortUtil.closeSerialPort();
    }
}
