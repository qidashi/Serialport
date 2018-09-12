package android_serialport_api;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Google官方代码
 * 此类的作用为，JNI的调用，用来加载.so文件的
 * 获取串口输入输出流
 */
@SuppressWarnings("all")
public class SerialPort {

    private static final String TAG = "SerialPort";

    /*
     * Do not remove or rename the field mFd: it is used                                                                                                                                                                            by native method
     * close();
     */
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    public SerialPort(File device, int baudRate, int flags)
            throws SecurityException, IOException {
		/* Check access permission */
        if (!device.canRead() || !device.canWrite()) {
            try {
                /* Missing read/write permission, trying to chmod the file */
                Process su;
                su = Runtime.getRuntime().exec("/system/bin/su");
                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"+ "exit\n";
                su.getOutputStream().write(cmd.getBytes());
                if ((su.waitFor() != 0) || !device.canRead() || !device.canWrite()) {
                    throw new SecurityException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new SecurityException();
            }
        }

        System.out.println(device.getAbsolutePath() + "==============================");
        mFd = open(device.getAbsolutePath(), baudRate, flags);// 关键代码，打开串口
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    // Getters and setters
    public InputStream getInputStream() {
        return mFileInputStream;
    }
    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    // JNI
    static {
        System.loadLibrary("serial_port");
    }
    private native static FileDescriptor open(String path, int baudRate,int flags);
    public native void close();
}
