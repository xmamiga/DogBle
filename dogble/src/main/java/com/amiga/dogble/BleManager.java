package com.amiga.dogble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.amiga.dogble.callback.BleScanCallback;
import com.amiga.dogble.callback.BluethoothAdapterStateChangCallback;
import com.amiga.dogble.callback.ReceiveDataCallback;
import com.amiga.dogble.util.ConnectInfo;
import com.amiga.dogble.util.Constant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Date: 2018/10/12-19:56
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class BleManager {
    private static final String TAG = "BleManager";

    Service mBleService;
    BluetoothAdapter mBluetoothAdapter;
    public BleScanner mBleScanner;
    public BleConDiscon mBleConDiscon;
    public BleServiceCommunicate mBleServiceCommunicate;
    public BleDataTransition mBleDataTransition;
    static List<ConnectInfo> mConnectInfoList = new ArrayList<ConnectInfo>();

    //bluethooth state
    private int mBluethoothState = BluetoothAdapter.STATE_OFF;

    BluethoothAdapterStateChangCallback mBAStateChangCb;


    /**
     * <p>Title: </p>
     * <p>Description: </p>
     */
    public BleManager(Service service) {
        super();
        // TODO Auto-generated constructor stub
        mBleService = service;
        mBleScanner = new BleScanner(this);
        mBleConDiscon = new BleConDiscon(this);
        mBleServiceCommunicate = new BleServiceCommunicate(this);
        mBleDataTransition = new BleDataTransition(this);
    }

    public boolean init() {
        BluetoothManager mBluetoothManager;
        mBluetoothManager = (BluetoothManager) mBleService.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            return false;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, "BluetoothAdapter is not enable");
            return false;
        }
        removeAllConnectInfo(mConnectInfoList);
        mBleScanner.init(mBluetoothAdapter);
        mBleConDiscon.init(mBluetoothAdapter, mConnectInfoList);
        mBleServiceCommunicate.init(mBluetoothAdapter, mConnectInfoList);
        mBleDataTransition.init(mBluetoothAdapter, mConnectInfoList);
        mBleConDiscon.setBleDataTransition(mBleDataTransition);
        return true;
    }

    public void setCallback(BleScanCallback bleScanCallback,
                            ReceiveDataCallback receiveDataCallback,
                            BluethoothAdapterStateChangCallback bAStateChangCb) {
        mBleScanner.setScanCallback(bleScanCallback);
        mBleConDiscon.setReceiveDataCallback(receiveDataCallback);
        mBAStateChangCb = bAStateChangCb;
    }


    public static int addConnecInfo(List<ConnectInfo> connectInfoList,
                                    ConnectInfo connectInfo) {
        synchronized (mConnectInfoList) {
            if (connectInfoList.size() >= Constant.MAX_CONNECT_SIZE) {
                Log.e(TAG, "connectInfoList is full");
                return Constant.ADD_LIST_FAIL_MAX_SIZE;
            }
            for (int i = 0; i < connectInfoList.size(); i++) {
                ConnectInfo tempConnectInfo = connectInfoList.get(i);
                if (tempConnectInfo.getMacAddr().equals(connectInfo.getMacAddr())) {
                    Log.e(TAG, "connectInfoList already contain this connectInfo");
                    return Constant.ADD_LIST_FAIL_EXIST_CONNECTINOF;
                }
            }
            connectInfoList.add(connectInfo);

            Log.i(TAG, "connectInfoList.add:" + connectInfo.getMacAddr());

            return Constant.ADD_LIST_SUCCESS;
        }

    }

    public static ConnectInfo getConnectInfo(List<ConnectInfo> connectInfoList,
                                             String macAddr) {
        if (connectInfoList == null) {
            return null;
        }
        synchronized (mConnectInfoList) {
            for (int i = 0; i < connectInfoList.size(); i++) {
                ConnectInfo tempConnectInfo = connectInfoList.get(i);
                if (tempConnectInfo.getMacAddr().equals(macAddr)) {
                    return tempConnectInfo;
                }
            }
            return null;
        }
    }

    public static void removeConnectInfo(List<ConnectInfo> connectInfoList,
                                         String macAddr) {
        synchronized (mConnectInfoList) {
            Iterator<ConnectInfo> iterator = connectInfoList.iterator();
            while (iterator.hasNext()) {
                ConnectInfo connectInfo = (ConnectInfo) iterator.next();
                if (connectInfo.getMacAddr().equals(macAddr)) {
                    iterator.remove();
                }
            }
            Log.i(TAG, "connectInfoList.remove:" + macAddr);
        }
    }


    public static void removeAllConnectInfo(List<ConnectInfo> connectInfoList) {
        synchronized (mConnectInfoList) {
            connectInfoList.clear();
            Log.i(TAG, "connectInfoList.clear:");
        }
    }


    public int getBluethoothState() {
        if (mBluetoothAdapter != null)
            return mBluetoothAdapter.getState();
        else
            return BluetoothAdapter.STATE_OFF;
    }

    private class BleAdapterBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                mBluethoothState = intent.getExtras().getInt(BluetoothAdapter.EXTRA_STATE);
                if (mBluethoothState == BluetoothAdapter.STATE_ON) {
                    Log.i(TAG, "STATE_ON init()=" + init());
                } else if (mBluethoothState == BluetoothAdapter.STATE_OFF) {
                    removeAllConnectInfo(mConnectInfoList);
                    mBleDataTransition.clearSend();
                    mBleScanner.setScanState(Constant.SCAN_IDLE);
                }
                mBAStateChangCb.onBluethoothAdapterState(mBluethoothState);
            }
        }
    }

    BleAdapterBroadcastReceiver bleAdapterBroadcastReceiver = new BleAdapterBroadcastReceiver();

    public void registerBluetoothAdapterBroastReciver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mBleService.registerReceiver(bleAdapterBroadcastReceiver, intentFilter);
    }

    public void unRegisterBluetoothAdapterBroastReciver() {
        mBleService.unregisterReceiver(bleAdapterBroadcastReceiver);
    }

    public boolean openBle() {
        if (mBluetoothAdapter != null) {
            Log.e(TAG, "mBluetoothAdapter.enable()");
            return mBluetoothAdapter.enable();
        }

        return false;
    }

    public boolean closeBle() {
        if (mBluetoothAdapter != null) {
            Log.e(TAG, "mBluetoothAdapter.disable()");
            return mBluetoothAdapter.disable();
        }

        return false;
    }
}
