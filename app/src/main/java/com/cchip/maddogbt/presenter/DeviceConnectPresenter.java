package com.cchip.maddogbt.presenter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.cchip.maddogbt.activity.MainActivity;
import com.cchip.maddogbt.bean.DeviceScanBean;
import com.cchip.maddogbt.ble.BleApiConfig;
import com.cchip.maddogbt.model.DeviceConnectModel;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Date: 2018/10/17-11:30
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class DeviceConnectPresenter {
    private static final String TAG = "DeviceConnectPresenter";

    Context mContext;
    MainActivity mActivity;
    DeviceConnectModel mDeviceConnectModel;
    private ArrayList<DeviceScanBean> deviceScanList = new ArrayList<DeviceScanBean>();

    public DeviceConnectPresenter(MainActivity activity,
                                  BleApiConfig bleApiBtDevice) {
        // TODO Auto-generated constructor stub
        mActivity = activity;
        mContext = mActivity.getApplicationContext();
        mDeviceConnectModel = new DeviceConnectModel(mContext, bleApiBtDevice,
                this);
    }

    public void unRegistBroadCastReceive() {
        mDeviceConnectModel.unRegistBroadCastReceive();
    }

    // scan
    public boolean isDeviceConnected(String addr) {
        return mDeviceConnectModel.isDeviceConnected(addr);
    }

    public boolean isDeviceConnecting(String addr) {
        return mDeviceConnectModel.isDeviceConnecting(addr);
    }

    public void startScan() {

        deviceScanList.clear();
        mActivity.notifyScanAdapterDataChange(deviceScanList);
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                Log.e(TAG, "startScan");
                final boolean state = mDeviceConnectModel.startScan();
                Log.e(TAG, "startScan:state : " + state);

            }
        }).start();
    }

    public void stopScan() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                Log.e(TAG, "stopScan");
                boolean success = mDeviceConnectModel.stopScan();
                Log.e(TAG, "stopScan : " + success);
            }
        }).start();

    }

    public void addDevice(BluetoothDevice device, final int rssi,
                          ScanResult scanResult) {
        boolean deviceFound = false;

        if ("".equals(device.getName()) || device.getName() == null) {
            return;
        }
        synchronized (deviceScanList) {
            for (DeviceScanBean listDev : deviceScanList) {
                if (listDev.getMacAddress().equals(device.getAddress())) {
                    deviceFound = true;
                    listDev.setRssi(rssi);
                    break;
                }
            }
        }
        if (!deviceFound) {
            Log.e(TAG, "newdevice:" + device.getAddress() + "  " + device.getName());
            Log.e(TAG, "ScanResult:" + scanResult.toString());
            Log.e(TAG, "scanRecord:" + scanResult.getScanRecord().toString());
            Log.e(TAG, "scanRecord getManufacturerSpecificData:" + scanResult.getScanRecord().getManufacturerSpecificData().toString());
            final DeviceScanBean dsb = new DeviceScanBean(device, rssi,
                    scanResult);
            deviceScanList.add(dsb);
            mActivity.notifyScanAdapterDataChange(deviceScanList);
        }
    }

    // connect
    public void connectDevice(String addr) {
        mDeviceConnectModel.connectDevice(addr);
    }

    // connect cmd send success, connecting
    public void connectDeviceSuccess(String addr) {
        mActivity.notifyScanAdapterDataChange(deviceScanList);
    }

    public void connectDeviceFail(String addr) {
        mActivity.connectDeviceFail();
    }

    // connected
    public void deviceConnected(String addr) {
        mActivity.connectDeviceSuccess(addr);
    }

    public void deviceDisConnected(String addr) {
        mActivity.connectDeviceFail();
    }

    public String SparseArrayToString(SparseArray<byte[]> array) {
        if (array == null) {
            return "null";
        }
        if (array.size() == 0) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append('{');
        for (int i = 0; i < array.size(); ++i) {
            buffer.append(array.keyAt(i)).append("=").append(Arrays.toString(array.valueAt(i)));
        }
        buffer.append('}');
        return buffer.toString();
    }

}
