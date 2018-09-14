package com.yuncai.serialport;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android_serialport_api.SerialPort;

/**
 * 通过串口用于接收或发送数据
 */

public class SerialPortUtil {

    private SerialPort serialPort = null;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private ReceiveThread mReceiveThread = null;
    private boolean isStart = false;
    // singleton
    private SerialPortUtil() {
    }
    private static class SingletonHandler {
        private static SerialPortUtil singleton = new SerialPortUtil();
    }
    public static SerialPortUtil getInstance(){
        return SingletonHandler.singleton;
    }

    /**
     * 打开串口，接收数据
     * 通过串口，接收单片机发送来的数据
     * @param portPath
     * @param baudRate
     */
    public void openSerialPort(String portPath, int baudRate) {
        try {
            serialPort = new SerialPort(new File(portPath), baudRate, 0);
            // 调用对象SerialPort方法，获取串口中"读和写"的数据流
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            isStart = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        getSerialPort();
    }

    /**
     * 关闭串口
     * 关闭串口中的输入输出流
     */
    public void closeSerialPort() {
        Log.i("test", "关闭串口");
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            isStart = false;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 发送数据
     * 通过串口，发送数据到单片机
     * @param data 要发送的数据
     */
    public void sendSerialPort(String data) {
        LogUtils.e("send data:"+data);
        try {
            byte[] sendData = DataUtils.HexToByteArr(data);
            outputStream.write(sendData);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getSerialPort() {
        if (mReceiveThread == null) {
            mReceiveThread = new ReceiveThread();
        }
        mReceiveThread.start();
    }

    /**
     * 4.接收串口数据的线程
     */
    private class ReceiveThread extends Thread {
        @Override
        public void run() {
            String receiveCmd = "";
            String HEAD = "A055";
            StringBuilder sb = new StringBuilder();
            //条件判断，只要条件为true，则一直执行这个线程
            while (isStart) {
                if (inputStream == null) {
                    return;
                }
                byte[] readData = new byte[1024*4];
                try {
                    // 为了一次性读完，做了延迟读取
                    if (inputStream.available() > 0 ) {
                        SystemClock.sleep(200);
                        int size = inputStream.read(readData);
                        if (size > 0) {
                            String readString = DataUtils.ByteArrToHex(readData, 0, size);
                            LogUtils.d(readString);
                            if(!TextUtils.isEmpty(readString)){
                                String[] split = readString.split(HEAD);
                                for (int i = 1; i < split.length; i++) {
                                    receiveCmd = HEAD+split[i];
                                    if(checkSum(receiveCmd)){
                                     sb.append(receiveCmd+"\n");
                                    }
                                    LogUtils.e(receiveCmd);
                                }
                            }
                            EventBus.getDefault().post(sb.toString());
                            sb.setLength(0);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * CheckSum  异或校验接收数据的正确性
     * @param receiveCmd
     * @return
     */
    private boolean checkSum(String receiveCmd) {
        String checkSum = receiveCmd.substring(receiveCmd.length() - 2);
        byte checkSumByte = DataUtils.HexToByte(checkSum);
        String substring = receiveCmd.substring(4).replaceAll(checkSum,"");
        int len = substring.length();
        byte temp= DataUtils.HexToByte(substring.substring(0,2));
        for (int i = 2; i < len; i+=2) {// 04 51 00 00
            String value = substring.substring(i, i + 2);
            temp ^= DataUtils.HexToByte(value);
        }
        LogUtils.e("校验：CheckSum========"+checkSumByte+"++++++++++++"+temp);
        return checkSumByte == temp;
    }

}
