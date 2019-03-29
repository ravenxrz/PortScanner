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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SetsScannerFragment extends Fragment {

    EditText portSetsEdit;
    TextView resultTv;
    final int threadNumber = 10;


    final int flushUI = 0;
    final int endScan = -1;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (flushUI == msg.what) {
                resultTv.append("\n" + msg.arg1 + "端口开启");
            } else if (endScan == msg.what) {
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
        View view = inflater.inflate(R.layout.fragment_sets_scan, container, false);
        portSetsEdit = view.findViewById(R.id.port_sets_edit);
        resultTv = view.findViewById(R.id.result);
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    public void startScan(String ip) {
        /* 置空显示 */
        resultTv.setText("扫描结果:");
        /* 获取端口集合 */
        Set<Integer> portSet = gainPortSet();
        if (null == portSet) {
            Toast.makeText(getContext(), "还未输入端口", Toast.LENGTH_SHORT).show();
            callBackListener.scanEndCallback();
            return;
        }
        final ExecutorService threadPool = Executors.newCachedThreadPool();
        for (int i = 0; i < threadNumber; i++) {
            SetScanner scanMethod2 = new SetScanner(ip, portSet,
                    threadNumber, i, 1000);
            threadPool.execute(scanMethod2);
        }
        threadPool.shutdown();
        /* 检查是否线程池关闭与否 */
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 每秒中查看一次是否已经扫描结束
                while (true) {
                    if (threadPool.isTerminated()) {
                        Log.i("Fragment", "扫描结束");
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

    private Set<Integer> gainPortSet() {
        String portsStr = portSetsEdit.getText().toString().trim();
        if ("".equals(portsStr)) {
            return null;
        }
        String[] ports = portsStr.split(",");
        Set<Integer> portSets = new HashSet<>();
        for (String port : ports) {
            try {
                portSets.add(Integer.valueOf(port));
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "已忽略过大端口号", Toast.LENGTH_SHORT).show();
            }

        }
        return portSets;
    }

    private class SetScanner implements Runnable {
        private String ip; // 目标IP
        private Set<Integer> portSet; // 待扫描的端口的Set集合
        private int threadNumber, serial, timeout; // 线程数，这是第几个线程，超时时间

        public SetScanner(String ip, Set<Integer> portSet, int threadNumber,
                          int serial, int timeout) {
            this.ip = ip;
            this.portSet = portSet;
            this.threadNumber = threadNumber;
            this.serial = serial;
            this.timeout = timeout;
        }

        public void run() {
            int port = 0;
            Integer[] ports = portSet.toArray(new Integer[portSet.size()]); // Set转数组
            try {
                InetAddress address = InetAddress.getByName(ip);
                Socket socket;
                SocketAddress socketAddress;
                if (ports.length < 1)
                    return;
                for (port = 0 + serial; port <= ports.length - 1; port += threadNumber) {
                    socket = new Socket();
                    socketAddress = new InetSocketAddress(address, ports[port]);
                    try {
                        socket.connect(socketAddress, timeout);
                        socket.close();
                        Message msg = Message.obtain();
                        msg.what = flushUI;
                        msg.arg1 = ports[port];
                        handler.sendMessage(msg);
                        Log.i("scan thread", port + "open");
                    } catch (IOException e) {
                        // System.out.println("端口 " + ports[port] + " ：关闭");
                        Log.i("scan thread", port + "close");
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

        }

    }
}
