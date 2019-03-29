package com.raven.portscanner;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RangeScannerFragment extends Fragment {

    EditText startPortEdit,endPortEdit;
    TextView resultTv;
    final int threadNumber = 100;


    final int flushUI = 0;
    final int endScan = -1;
    @SuppressLint("HandlerLeak")
    private  Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(flushUI == msg.what){
                resultTv.append("\n"+msg.arg1+"端口开启");
            }else if (endScan == msg.what){
                callBackListener.scanEndCallback();
            }
        }
    };


    private ScannerEndCallBackListener callBackListener;

    public void setCallBackListener(ScannerEndCallBackListener callBackListener) {
        this.callBackListener = callBackListener;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_port_range,container,false);
        startPortEdit = view.findViewById(R.id.start_port_edit);
        endPortEdit = view.findViewById(R.id.end_port_edit);
        resultTv = view.findViewById(R.id.result);
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    public void startScanner(String ip){
        resultTv.setText("扫描结果:");
        String startStr = startPortEdit.getText().toString().trim();
        String endStr = endPortEdit.getText().toString().trim();
        if("".equals(startStr) || "".equals(endStr)){
            Toast.makeText(getContext(),"端口信息不完整",Toast.LENGTH_SHORT).show();
            callBackListener.scanEndCallback();
            return;
        }
        int startPort = Integer.valueOf(startStr);
        int endPort = Integer.valueOf(endPortEdit.getText().toString().trim());
        if(startPort > endPort){
            Toast.makeText(getContext(),"开始端口大于结束端口",Toast.LENGTH_SHORT).show();
            callBackListener.scanEndCallback();
            return;
        }
        final ExecutorService threadPool = Executors.newCachedThreadPool();
        for (int i = 0; i < threadNumber; i++) {
            RangeScanner scanMethod1 = new RangeScanner(ip,
                    startPort,endPort,threadNumber,i,1000);
            threadPool.execute(scanMethod1);
        }
        threadPool.shutdown();

        /* 检查是否线程池关闭与否 */
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 每秒中查看一次是否已经扫描结束
                while (true) {
                    if (threadPool.isTerminated()) {
                        Log.i("Fragment","扫描结束");
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                handler.sendEmptyMessage(endScan);
            }
        }).start();

    }


    class RangeScanner implements Runnable {
        private String ip; // 目标IP
        private int startPort, endPort, threadNumber, serial, timeout; // 起始和结束端口，线程数，这是第几个线程，超时时间

        /**
         * 初始化
         *
         * @param ip
         *            待扫描IP或域名
         * @param startPort
         *            起始端口
         * @param endPort
         *            结束端口
         * @param threadNumber
         *            线程数
         * @param serial
         *            标记是第几个线程
         * @param timeout
         *            连接超时时间
         * */
        public RangeScanner(String ip, int startPort, int endPort,
                           int threadNumber, int serial, int timeout) {
            this.ip = ip;
            this.startPort = startPort;
            this.endPort = endPort;
            this.threadNumber = threadNumber;
            this.serial = serial;
            this.timeout = timeout;
        }

        public void run() {
            int port = 0;
            try {
                InetAddress address = InetAddress.getByName(ip);
                Socket socket;
                SocketAddress socketAddress;
                for (port = startPort + serial; port <= endPort; port += threadNumber) {
                    socket = new Socket();
                    socketAddress = new InetSocketAddress(address, port);
                    try {
                        socket.connect(socketAddress, timeout); // 超时时间
                        socket.close();
                        Log.i("scanner thread",port+"open");
                        Message msg = Message.obtain();
                        msg.what = flushUI;
                        msg.arg1 = port;
                        handler.sendMessage(msg);
                    } catch (IOException e) {
                        Log.i("scanner thread",port+"close");
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

    }
}
