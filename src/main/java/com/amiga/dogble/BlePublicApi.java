package com.amiga.dogble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.amiga.dogble.callback.BleScanCallback;
import com.amiga.dogble.callback.BluethoothAdapterStateChangCallback;
import com.amiga.dogble.callback.ConnectStateCallback;
import com.amiga.dogble.callback.ReceiveDataCallback;
import com.amiga.dogble.util.Constant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Date: 2018/10/13-12:12
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public abstract class BlePublicApi extends Service {
    private static final String TAG = "BlePublicApi";

    public static final String BLEPUBLICAPI_VERSION = "V1.0";

    private static final int MSG_WHAT_SCAN_DEVICE_FOUND = 10010;
    private static final String MSG_BUNDLE_KEY_SCAN_DEVICE_ADDRESS = "MSG_BUNDLE_KEY_SCAN_DEVICE_ADDRESS";

    BleManager mBleManager;
    BleScanCallback mBleScanCallbackToUI;
    ConnectStateCallback mConnectCallbackToUI;

    AutoReConnect mAutoReConnect;
    private boolean restartBle = false;
    private boolean needAutoReConnect = true;
    private boolean needConnectStatusBroadcast = true;
    private boolean needClearReConnectSetAfterBleOff = true;

    private boolean isUiScanning = false;
    private HashMap<String, Timer> scanTimerMap = new HashMap<String, Timer>();

    private int scanTimeBeforeConnect = 6000;

    Handler mHandler = new Handler() {
        public void dispatchMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_WHAT_SCAN_DEVICE_FOUND:
                    String addr = msg.getData().getString(MSG_BUNDLE_KEY_SCAN_DEVICE_ADDRESS);
                    Timer timer = scanTimerMap.get(addr);
                    if (timer != null) {
                        synchronized (scanTimerMap) {
                            timer.cancel();
                            scanTimerMap.remove(addr);
                        }

                        Log.e(TAG, "mHandler remove:" + addr);
                        if (scanTimerMap.isEmpty()) {
                            stopScanBeforeConnect();
                        }

                        reallyConnect(addr);
                    }
                    break;

                default:
                    break;
            }
        }

        ;
    };

    BleScanCallback mBleScanCallbackToBleSdk = new BleScanCallback() {

        @Override
        public void onScanCallback(int callbackType, ScanResult result) {

            if (mBleScanCallbackToUI != null) {
                mBleScanCallbackToUI.onScanCallback(callbackType, result);
            }

            if (needAutoReConnect) {
                if (mAutoReConnect.needAutoConnect(result.getDevice().getAddress())) {
                    Bundle bundle = new Bundle();
                    bundle.putString(MSG_BUNDLE_KEY_SCAN_DEVICE_ADDRESS, result.getDevice().getAddress());
                    Message msg = new Message();
                    msg.what = MSG_WHAT_SCAN_DEVICE_FOUND;
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                }
            }
        }
    };


    final ConnectStateCallback mConnectStateCallbackToSdk = new ConnectStateCallback() {

        @Override
        public void onConnectStateCallback(final String addr, int state) {
            // TODO Auto-generated method stub
            Log.i(TAG, addr + "  state=" + getStateString(state));

            switch (state) {
                case Constant.CONNECTING:
                    break;

                case Constant.CONNECTED:
                    break;

                case Constant.CONNECT_TIMEOUT:
                    commucateInit(addr);
                    handleClose(addr);
                    break;

                case Constant.DISCONNECTING:
                    break;

                case Constant.DISCONNECTED:
                    commucateInit(addr);
                    handleClose(addr);
                    break;

                case Constant.DISCONNECT_TIMEOUT:
                    commucateInit(addr);
                    handleClose(addr);
                    break;

                case Constant.DISCOVERY_SERVICE_ING:
                    break;

                case Constant.DISCOVERY_SERVICE_OK:
                    if (getCommunication(addr)) {
                        sendCmdAfterConnected(addr);
                        state = Constant.COMMUNICATE_SUCCESS;
                    } else {
                        state = Constant.COMMUNICATE_FAIL;
                        commucateInit(addr);
                        mBleManager.mBleConDiscon.disconnect(addr);
                    }
                    break;

                case Constant.DISCOVERY_SERVICE_FAIL:
                    Log.e(TAG, "service found error");
                    state = Constant.COMMUNICATE_FAIL;
                    commucateInit(addr);
                    mBleManager.mBleConDiscon.disconnect(addr);
                    break;

                case Constant.CONNECT_ERROR_NEEDTO_CLOSE_BLE:
                    restartBle = true;
                    mBleManager.closeBle();
                    break;

                default:
                    break;
            }

            sendStateToUi(addr, state);
            onAutoConnectStateCallback(addr, state);
            sendConnectStateBroadcast(addr, state);
        }
    };

    BluethoothAdapterStateChangCallback mBAStateChangeCb = new BluethoothAdapterStateChangCallback() {
        public void onBluethoothAdapterState(int state) {
            Log.e(TAG, "BluetoothAdapter state is " + state);
            if (state != BluetoothAdapter.STATE_ON) {
                commucateInitAall();
            }

            if (state == BluetoothAdapter.STATE_ON) {
                Intent intent = new Intent();
                intent.setAction(Constant.ACTION_BLUETHOOTH_STATE_CHANGE);
                intent.putExtra(Constant.EXTRA_BLUETHOOTH_STATE_CHANGE, Constant.BLUETHOOTH_STATE_ON);
                sendBroadcast(intent);

                autoReConnect();
            } else if (state == BluetoothAdapter.STATE_OFF) {
                Intent intent = new Intent();
                intent.setAction(Constant.ACTION_BLUETHOOTH_STATE_CHANGE);
                intent.putExtra(Constant.EXTRA_BLUETHOOTH_STATE_CHANGE, Constant.BLUETHOOTH_STATE_OFF);
                sendBroadcast(intent);
                clearAllStopScanTimer();
                if (needClearReConnectSetAfterBleOff) {
                    mAutoReConnect.clearAutoConnectSet();
                }
                if (restartBle) {
                    restartBle = false;
                    mBleManager.openBle();
                }
            }
        }

        ;
    };

    ReceiveDataCallback mReceiveDataCallback = new ReceiveDataCallback() {

        @Override
        public void onReceiveData(String macAddr,
                                  BluetoothGattCharacteristic characteristic) {
            // TODO Auto-generated method stub
            prasedata(macAddr, characteristic);
        }
    };


    private void handleClose(String addr) {
        mBleManager.mBleConDiscon.closeGatt(addr);
    }


    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        Log.e(TAG, "oncreat");
        mAutoReConnect = new AutoReConnect(this);
        mBleManager = new BleManager(this);
        mBleManager.registerBluetoothAdapterBroastReciver();
        init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        Log.e(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // TODO Auto-generated method stub
        Log.e(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        // TODO Auto-generated method stub
        Log.e(TAG, "onRebind");
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        clearAllStopScanTimer();
        mAutoReConnect.clearAutoConnectSet();
        mBleManager.mBleConDiscon.disconnectAll();
        mBleManager.unRegisterBluetoothAdapterBroastReciver();
        mBleManager = null;
    }

    //return false need restart bluethooth
    public boolean init() {
        boolean result = mBleManager.init();
        mBleManager.setCallback(mBleScanCallbackToBleSdk, mReceiveDataCallback,
                mBAStateChangeCb);

        return result;
    }

    /**
     * @param @return
     * @return boolean
     * @throws
     * @Title: openBle
     * @Description: TODO
     */
    public boolean openBle() {
        return mBleManager.openBle();
    }

    /**
     * @param @return
     * @return boolean
     * @throws
     * @Title: closeBle
     * @Description: TODO
     */
    public boolean closeBle() {
        return mBleManager.closeBle();
    }


    /**
     * @param @param  bleScanCallback
     * @param @return
     * @return int    Constant.SUCCESS
     * Constant.FAIL
     * Constant.ALREADY_SCAN_START
     * Constant.FAIL_ADAPTER
     * @throws
     * @Title: startScan
     * @Description: TODO
     */
    public int startScan(BleScanCallback bleScanCallback) {
        mBleScanCallbackToUI = bleScanCallback;
        isUiScanning = true;
        int state = mBleManager.mBleScanner.startScan(null);
        return state;
    }

    /**
     * @param @param bleScanCallback
     * @param @param services
     * @return int    Constant.SUCCESS
     * Constant.FAIL
     * Constant.ALREADY_SCAN_START
     * Constant.FAIL_ADAPTER
     * @throws
     * @Title: startScanFilterByService
     * @Description: TODO
     */
    protected int startScanFilterByService(BleScanCallback bleScanCallback, UUID[] services) {
        mBleScanCallbackToUI = bleScanCallback;
        return mBleManager.mBleScanner.startScan(services);
    }


    /**
     * @param @param  bleScanCallback
     * @param @return
     * @return int    Constant.SUCCESS
     * Constant.ALREADY_SCAN_STOP
     * Constant.FAIL_ADAPTER
     * @throws
     * @Title: stopScan
     * @Description: TODO
     */
    public int stopScan(BleScanCallback bleScanCallback) {

        isUiScanning = false;

        int state = mBleManager.mBleScanner.stopScan();
        if (state == Constant.SUCCESS) {
            mBleScanCallbackToUI = null;
        }
        return state;
    }


    /**
     * @param @param  addr
     * @param @return
     * @return int   Constant.SUCCESS
     * Constant.FAIL
     * Constant.FAIL_PARAMETER
     * Constant.ALREDY_DISCONNECT
     * Constant.FAIL_ADAPTER
     * @throws
     * @Title: disconnect
     * @Description: TODO
     */
    public int disconnect(String addr) {
        if (!BluetoothAdapter.checkBluetoothAddress(addr)) {
            Log.e(TAG, addr + ":  MacAddress error");
            return Constant.FAIL_PARAMETER;
        }

        removeAutoConnectSet(addr);
        if (mBleManager == null || mBleManager.mBleConDiscon == null) {
            return Constant.FAIL;
        } else {
            return mBleManager.mBleConDiscon.disconnect(addr);
        }
    }

    private int disconnectCauseByConnectTimeout(String addr) {
        if (!BluetoothAdapter.checkBluetoothAddress(addr)) {
            Log.e(TAG, addr + ":  MacAddress error");
            return Constant.FAIL_PARAMETER;
        }
        return mBleManager.mBleConDiscon.disconnect(addr);
    }


    /**
     * @param @param  addr
     * @param @param  connectStateCallback
     * @param @return
     * @return int   Constant.SUCCESS
     * Constant.FAIL_PARAMETER
     * Constant.ALREDY_CONNECT
     * Constant.FAIL_ADAPTER
     * Constant.MAX_CONNECT
     * @throws
     * @Title: autoConnect
     * @Description: TODO
     */
    public int autoConnect(String addr, ConnectStateCallback connectStateCallback) {
        if (!BluetoothAdapter.checkBluetoothAddress(addr)) {
            Log.e(TAG, addr + ":  MacAddress error");
            return Constant.FAIL_PARAMETER;
        }

        addAutoConnectSet(addr);
        mConnectCallbackToUI = connectStateCallback;
        return connect(addr);
    }

    public int autoConnectNotSetCallback(String addr) {
        if (!BluetoothAdapter.checkBluetoothAddress(addr)) {
            Log.e(TAG, addr + ":  MacAddress error");
            return Constant.FAIL_PARAMETER;
        }

        addAutoConnectSet(addr);
        return connect(addr);
    }

    public int autoConnectDirectly(String addr, ConnectStateCallback connectStateCallback) {
        if (!BluetoothAdapter.checkBluetoothAddress(addr)) {
            Log.e(TAG, addr + ":  MacAddress error");
            return Constant.FAIL_PARAMETER;
        }

        addAutoConnectSet(addr);
        mConnectCallbackToUI = connectStateCallback;
        return reallyConnect(addr);
    }


    int connect(String addr) {

        int state = getConnectState(addr);
        if (state != Constant.CONNECT_IDLE) {
            Log.e(TAG, "already in connectlist");
            return Constant.SUCCESS;
        }

        if (needAutoReConnect) {
            startScanBeforeConnect(addr);
        } else {
            reallyConnect(addr);
        }

        return Constant.SUCCESS;
    }


    private int reallyConnect(String addr) {
        Log.i(TAG, "start connecting");
        int state = mBleManager.mBleConDiscon.connect(addr, mConnectStateCallbackToSdk);
        Log.i(TAG, "connecting result =" + state);
        return state;
    }


    public BluetoothGattCharacteristic getWriteCharateristic(String macAddr,
                                                             UUID service, UUID characteristic) {
        return mBleManager.mBleServiceCommunicate
                .getChrateristic(macAddr, service, characteristic);
    }

    public boolean setCharateristicNotification(String macAddr,
                                                UUID service, UUID notifyCharacteristic) {
        return mBleManager.mBleServiceCommunicate.
                setNotificationCharateristic(macAddr, service, notifyCharacteristic);
    }

    public int getBleAdapterState() {
        if (mBleManager.getBluethoothState() == BluetoothAdapter.STATE_ON) {
            return Constant.BLUETHOOTH_STATE_ON;
        } else {
            return Constant.BLUETHOOTH_STATE_OFF;
        }
    }

    /**
     * @param @param  addr
     * @param @return
     * @return Constant.CONNECT_IDLE ;
     * Constant.CONNECTING ;
     * Constant.CONNECTED ;
     * Constant.CONNECT_TIMEOUT;
     * Constant.DISCONNECTING;
     * Constant.DISCONNECTED ;
     * Constant.DISCONNECT_TIMEOUT;
     * Constant.DISCOVERY_SERVICE_ING ;
     * Constant.DISCOVERY_SERVICE_OK ;
     * Constant.DISCOVERY_SERVICE_FAIL;
     * @throws
     * @Title: getConnectState
     * @Description: TODO
     */
    public int getConnectState(String addr) {
        int state = mBleManager.mBleConDiscon.getConnectState(addr);
        return state;
    }

    /**
     * @param @param  addr
     * @param @return
     * @return Constant.CONNET_IDLE
     * Constant.CONNETING
     * Constant.CONNETED
     * Constant.DISCONNETING
     * Constant.DISCONNETED
     * @throws
     * @Title: getDeviceConnectState
     * @Description: TODO
     */
    public int getDeviceConnectState(String addr) {
        int state = mBleManager.mBleConDiscon.getConnectState(addr);
        return transformConnectStateToUiQuery(state);
    }

    public boolean isInAutoReconnectSet(String addr) {

        return mAutoReConnect.needAutoConnect(addr);
    }

    private int transformConnectStateToUiQuery(int state) {

        if (state == Constant.CONNECTING
                || state == Constant.CONNECTED
                || state == Constant.DISCOVERY_SERVICE_ING) {
            return Constant.CONNECTING;
        } else if (state == Constant.DISCOVERY_SERVICE_OK) {
            return Constant.CONNECTED;
        } else if (state == Constant.DISCOVERY_SERVICE_FAIL
                || state == Constant.DISCONNECTING) {
            return Constant.DISCONNECTING;
        } else if (state == Constant.DISCONNECTED
                || state == Constant.CONNECT_TIMEOUT
                || state == Constant.DISCONNECT_TIMEOUT) {
            return Constant.DISCONNECTED;
        } else if (state == Constant.CONNECT_IDLE) {
            return Constant.CONNECT_IDLE;
        } else {
            return Constant.DISCONNECTED;
        }
    }


    private int transformConnectStateToUiReport(int state) {

        if (state == Constant.CONNECTING
                || state == Constant.CONNECTED
                || state == Constant.DISCOVERY_SERVICE_ING) {
            return Constant.CONNECTING;
        } else if (state == Constant.DISCOVERY_SERVICE_FAIL
                || state == Constant.DISCONNECTING) {
            return Constant.DISCONNECTING;
        } else if (state == Constant.DISCONNECTED
                || state == Constant.CONNECT_TIMEOUT
                || state == Constant.DISCONNECT_TIMEOUT) {
            return Constant.DISCONNECTED;
        } else if (state == Constant.COMMUNICATE_SUCCESS) {
            return Constant.CONNECTED;
        } else if (state == Constant.COMMUNICATE_FAIL) {
            return Constant.DISCONNECTING;
        } else {
            return Constant.DISCONNECTED;
        }

    }


    /**
     * @param @param  addr
     * @param @param  mWriteCharacteristic
     * @param @param  data
     * @param @return
     * @return boolean
     * @throws
     * @Title: writeData
     * @Description: TODO
     */
    public boolean writeData(String addr, BluetoothGattCharacteristic mWriteCharacteristic, ArrayList<byte[]> data) {

        if (!BluetoothAdapter.checkBluetoothAddress(addr)) {
            Log.i(TAG, "macaddress false");
            return false;
        }
        if (!isCommunicte(addr)) {
            Log.i(TAG, "isCommunicte false");
            return false;
        }
        return mBleManager.mBleDataTransition.writeData(addr, mWriteCharacteristic, data);
    }

    public boolean writeData(String addr, BluetoothGattCharacteristic mWriteCharacteristic, byte[] data) {

        ArrayList<byte[]> datas = new ArrayList<byte[]>();
        datas.add(data);

        if (!BluetoothAdapter.checkBluetoothAddress(addr)) {
            Log.i(TAG, "macaddress false");
            return false;
        }
        if (!isCommunicte(addr)) {
            Log.i(TAG, "isCommunicte false");
            return false;
        }
        return mBleManager.mBleDataTransition.writeData(addr, mWriteCharacteristic, datas);
    }

    /**
     * @param @param  addr
     * @param @param  mWriteCharacteristic
     * @param @return
     * @return boolean
     * @throws
     * @Title: readData
     * @Description: TODO
     */
    public boolean readData(String addr, BluetoothGattCharacteristic mWriteCharacteristic) {

        if (!BluetoothAdapter.checkBluetoothAddress(addr)) {
            Log.i(TAG, "macaddress false");
            return false;
        }
        if (!isCommunicte(addr)) {
            Log.i(TAG, "isCommunicte false");
            return false;
        }
        return mBleManager.mBleDataTransition.readData(addr, mWriteCharacteristic);
    }


    private void sendStateToUi(String addr, int state) {
        if (mConnectCallbackToUI == null) {
            Log.e(TAG, "mConnectCallbackToUI is null");
            return;
        }

        mConnectCallbackToUI.onConnectStateCallback(addr, transformConnectStateToUiReport(state));
    }

    public void setConnectCallbackToUI(ConnectStateCallback callback) {
        mConnectCallbackToUI = callback;
    }

    public void setNeedAutoReConnect(boolean needAutoReConnect) {
        this.needAutoReConnect = needAutoReConnect;
    }

    public void setNeedConnectStatusBroadcast(boolean needConnectStatusBroadcast) {
        this.needConnectStatusBroadcast = needConnectStatusBroadcast;
    }

    public void setNeedClearReConnectSetAfterBleOff(boolean needClearReConnectSetAfterBleOff) {
        this.needClearReConnectSetAfterBleOff = needClearReConnectSetAfterBleOff;
    }

    public void setScanTimeBeforConnect(int time) {
        this.scanTimeBeforeConnect = time;
    }

    public void setCmdSendIntervalMs(int intervalMs) {
        Log.e(TAG, "cmd send intervalMs:" + intervalMs);
        //mBleManager.mBleDataTransition.setCmdSendIntervalMS(intervalMs);
    }

    public void setMtu(int mtu) {
        Log.e(TAG, "cmd set MTU:" + mtu);
        mBleManager.mBleConDiscon.setMtu(mtu);
    }

    public int getMtu() {
        final int mtu = mBleManager.mBleConDiscon.getMtu();
        Log.e(TAG, "cmd get MTU:" + mtu);
        return mtu;
    }

    private void autoReConnect() {
        // TODO Auto-generated method stub
        if (needAutoReConnect) {
            mAutoReConnect.autoConnect();
        }
    }

    private void removeAutoConnectSet(String addr) {
        // TODO Auto-generated method stub
        if (needAutoReConnect) {
            mAutoReConnect.removeAutoConnectSet(addr);
        }
    }

    private void addAutoConnectSet(String addr) {
        // TODO Auto-generated method stub
        if (needAutoReConnect) {
            mAutoReConnect.addAutoConnectSet(addr);
        }
    }

    private void onAutoConnectStateCallback(String addr, int state) {
        // TODO Auto-generated method stub
        if (needAutoReConnect) {
            mAutoReConnect.onConnectStateCallback(addr, state);
        }
    }

    private void sendConnectStateBroadcast(String addr, int state) {
        // TODO Auto-generated method stub
        if (needConnectStatusBroadcast) {
            int tempstate = transformConnectStateToUiReport(state);
            if (tempstate == Constant.CONNECTED || tempstate == Constant.DISCONNECTED) {
                Log.i(TAG, "sendConnectStateBroadcast:" + tempstate);
                Intent intent = new Intent();
                intent.setAction(Constant.ACTION_DEVICE_CONNECT_STATUS);
                intent.putExtra(Constant.EXTRA_DEVICE_CONNECT_STATUS_ADDRESS, addr);
                intent.putExtra(Constant.EXTRA_DEVICE_CONNECT_STATUS, tempstate);
                sendBroadcast(intent);
            }
        }
    }


    private String getStateString(int state) {

        String stateString = "";
        switch (state) {
            case Constant.CONNECT_IDLE:
                stateString = "connect_idle";
                break;
            case Constant.CONNECTING:
                stateString = "connecting";
                break;
            case Constant.CONNECTED:
                stateString = "connected";
                break;
            case Constant.CONNECT_TIMEOUT:
                stateString = "connect_timeout";
                break;
            case Constant.DISCONNECTING:
                stateString = "disconnecting";
                break;
            case Constant.DISCONNECTED:
                stateString = "disconnected";
                break;
            case Constant.DISCONNECT_TIMEOUT:
                stateString = "disconnect_timeout";
                break;
            case Constant.DISCOVERY_SERVICE_ING:
                stateString = "discoverying";
                break;
            case Constant.DISCOVERY_SERVICE_OK:
                stateString = "discoverysuccess";
                break;
            case Constant.DISCOVERY_SERVICE_FAIL:
                stateString = "discoveryfail";
                break;

            default:
                break;
        }

        return stateString;
    }


    private void startScanBeforeConnect(final String addr) {
        if (mBleManager.mBleScanner.getScanState() == Constant.SCAN_IDLE) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    mBleManager.mBleScanner.startScan(null);
                }
            }).start();
        }
        synchronized (scanTimerMap) {
            if (!scanTimerMap.containsKey(addr)) {
                Timer stopScanTimeoutTimer = new Timer();
                stopScanTimeoutTimer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        scanTimerMap.remove(addr);
                        Log.e(TAG, "stopScanTimeoutTimer remove:" + addr);
                        if (scanTimerMap.isEmpty()) {
                            stopScanBeforeConnect();
                        }
                        mAutoReConnect.onConnectStateCallback(addr, Constant.CONNECT_SCAN_NOT_FOUND);
                    }
                }, scanTimeBeforeConnect);
                Log.e(TAG, "scanTimerMap put:" + addr);
                scanTimerMap.put(addr, stopScanTimeoutTimer);
            }
        }
    }

    private void stopScanBeforeConnect() {
        if (!isUiScanning) {
            mBleManager.mBleScanner.stopScan();
        }
    }

    private void clearAllStopScanTimer() {
        Log.i(TAG, "clearAllStopScanTimer");
        synchronized (scanTimerMap) {
            Iterator<Map.Entry<String, Timer>> iterator = scanTimerMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<java.lang.String, java.util.Timer> entry = (Map.Entry<java.lang.String, java.util.Timer>) iterator
                        .next();
                entry.getValue().cancel();
                iterator.remove();
            }
        }
    }

    public abstract void commucateInit(String addr);

    public abstract void commucateInitAall();

    public abstract boolean getCommunication(String addr);

    public abstract boolean isCommunicte(String addr);

    public abstract void sendCmdAfterConnected(String addr);

    public abstract void prasedata(String macAddr, BluetoothGattCharacteristic characteristic);

}
