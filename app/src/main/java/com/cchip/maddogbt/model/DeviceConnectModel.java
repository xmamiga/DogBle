package com.cchip.maddogbt.model;

import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.amiga.dogble.callback.BleScanCallback;
import com.amiga.dogble.callback.ConnectStateCallback;
import com.amiga.dogble.util.Constant;
import com.cchip.maddogbt.ble.BleApiConfig;
import com.cchip.maddogbt.presenter.DeviceConnectPresenter;

/**
 * Date: 2018/10/17-11:31
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class DeviceConnectModel {
    Context mContext;
    BleApiConfig mBleApiBtDevice;
    DeviceConnectPresenter mDeviceConnectPresenter;

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent.getAction().equals(
                    Constant.ACTION_BLUETHOOTH_STATE_CHANGE)) {
                int state = intent.getIntExtra(
                        Constant.EXTRA_BLUETHOOTH_STATE_CHANGE,
                        Constant.BLUETHOOTH_STATE_OFF);
            }
        }
    };

    BleScanCallback mBleScanCallback = new BleScanCallback() {

        @Override
        public void onScanCallback(int callbackType, ScanResult result) {
            mDeviceConnectPresenter.addDevice(result.getDevice(), result.getRssi(), result);
        }
    };


    ConnectStateCallback mConnectStateCallback = new ConnectStateCallback() {

        @Override
        public void onConnectStateCallback(String addr, int state) {
            // TODO Auto-generated method stub
            if (state == Constant.CONNECTED) {
                mDeviceConnectPresenter.deviceConnected(addr);
            } else if (state == Constant.DISCONNECTED) {
                mDeviceConnectPresenter.deviceDisConnected(addr);
            }
        }
    };

    public DeviceConnectModel(Context context, BleApiConfig bleApiBtDevice,
                              DeviceConnectPresenter deviceConnectPresenter) {
        // TODO Auto-generated constructor stub
        this.mContext = context;
        this.mBleApiBtDevice = bleApiBtDevice;
        this.mDeviceConnectPresenter = deviceConnectPresenter;
        registBroadCastReceive();
        mBleApiBtDevice.setConnectCallbackToUI(mConnectStateCallback);
    }

    private void registBroadCastReceive() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.ACTION_BLUETHOOTH_STATE_CHANGE);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    public void unRegistBroadCastReceive() {
        mBleApiBtDevice.setConnectCallbackToUI(null);
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

    public boolean openBle() {
        return mBleApiBtDevice.openBle();
    }

    public boolean closeBle() {
        return mBleApiBtDevice.closeBle();
    }

    // scan
    public boolean isBleOpen() {

        if (mBleApiBtDevice.getBleAdapterState() == Constant.BLUETHOOTH_STATE_ON) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isDeviceConnected(String addr) {
        int state = mBleApiBtDevice.getDeviceConnectState(addr);
        if (state == Constant.CONNECTED) {
            return true;
        }
        return false;
    }

    public boolean isDeviceConnecting(String addr) {
        int state = mBleApiBtDevice.getDeviceConnectState(addr);
        if (state == Constant.CONNECTING) {
            return true;
        } else if (state == Constant.CONNECT_IDLE) {
            return mBleApiBtDevice.isInAutoReconnectSet(addr);
        }

        return false;
    }

    public boolean startScan() {
        int state = mBleApiBtDevice.startScan(mBleScanCallback);

        if (state == Constant.SUCCESS || state == Constant.ALREADY_SCAN_START) {
            return true;
        }

        return false;
    }

    public boolean stopScan() {
        int state = mBleApiBtDevice.stopScan(null);

        if (state == Constant.SUCCESS || state == Constant.ALREADY_SCAN_STOP) {
            return true;
        }

        return false;
    }

    // connect
    public void connectDevice(String addr) {
        int state = mBleApiBtDevice.autoConnect(addr, mConnectStateCallback);
        if (state == Constant.SUCCESS || state == Constant.ALREDY_CONNECT) {
            if (state == Constant.SUCCESS) {
                mDeviceConnectPresenter.connectDeviceSuccess(addr);
            } else {
                mDeviceConnectPresenter.connectDeviceFail(addr);
            }
        }
    }
}
