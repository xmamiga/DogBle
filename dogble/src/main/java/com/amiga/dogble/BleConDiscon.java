package com.amiga.dogble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.util.Log;

import com.amiga.dogble.callback.ConnectStateCallback;
import com.amiga.dogble.callback.ReceiveDataCallback;
import com.amiga.dogble.util.BleUtils;
import com.amiga.dogble.util.ConnectInfo;
import com.amiga.dogble.util.Constant;
import com.amiga.dogble.util.TimeOut;

import java.util.Iterator;
import java.util.List;

/**
 * Date: 2018/10/13-11:34
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class BleConDiscon {

    private static final String TAG = "BleConDiscon";

    BluetoothAdapter mBluetoothAdapter;
    BleManager mBleManager;

    List<ConnectInfo> mConnectInfoList;
    //notify data prase
    ReceiveDataCallback mReciveDataCallback;
    BleDataTransition mBleDataTransition;


    public static boolean blocking;

    private static int mMtu = Constant.DEFAULT_MTU;

    public BleConDiscon(BleManager bleManager) {
        mBleManager = bleManager;
    }

    public void setMtu(int mtu) {
        this.mMtu = mtu;
    }

    public int getMtu() {
        return this.mMtu;
    }

    protected void setBleDataTransition(BleDataTransition bleDataTransition) {
        mBleDataTransition = bleDataTransition;
    }

    protected void init(BluetoothAdapter bluetoothAdapter, List<ConnectInfo> connectInfoList) {
        mBluetoothAdapter = bluetoothAdapter;
        this.mConnectInfoList = connectInfoList;
    }

    protected void setReceiveDataCallback(ReceiveDataCallback receiveDataCallback){
        mReciveDataCallback = receiveDataCallback;
    }

    public int getConnectState(String macAddr) {
        ConnectInfo connectInfo = BleManager.getConnectInfo(mConnectInfoList, macAddr);
        if (connectInfo == null) {
            return Constant.CONNECT_IDLE;
        }
        return connectInfo.getState();
    }

    public int connect(String macAddr, ConnectStateCallback connectStateCallback) {
        if (connectStateCallback == null) {
            Log.e(TAG, macAddr + ":  connectStateCallback null");
            return Constant.FAIL_PARAMETER;
        }

        if (!BluetoothAdapter.checkBluetoothAddress(macAddr)) {
            Log.e(TAG, macAddr + ":  MacAddress error");
            return Constant.FAIL_PARAMETER;
        }


        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            if (mBluetoothAdapter == null) {
                Log.e(TAG, "mBluetoothAdapter == null");
            }
            Log.e(TAG, macAddr + ":  FAIL_ADAPTER");
            return Constant.FAIL_ADAPTER;
        }

        int state = 0;
        ConnectInfo connectInfo = null;
        synchronized (this) {

            if (alreadyConnect(macAddr)) {
                Log.e(TAG, macAddr + ":  ALREDY_CONNECT");
                return Constant.ALREDY_CONNECT;
            }

            if (mConnectInfoList.size() >= Constant.MAX_CONNECT_SIZE) {
                Log.e(TAG, "list is full");
                return Constant.MAX_CONNECT;
            }


            final BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(macAddr);
            if (bluetoothDevice == null) {
                Log.e(TAG, "bluetoothDevice is null");
                return Constant.FAIL;
            }

            BluetoothGatt bluetoothGatt = bluetoothDevice.connectGatt(mBleManager.mBleService, false, mGattCallback);
            if (bluetoothGatt == null) {
                Log.e(TAG, "bluetoothGatt is null");
                return Constant.FAIL;
            }
            connectInfo = createConnectInfo(macAddr, connectStateCallback, bluetoothGatt);

            state = BleManager.addConnecInfo(mConnectInfoList, connectInfo);
        }
        Log.i(TAG, "connecting");
        if (state == Constant.ADD_LIST_SUCCESS) {
            connectInfo.getmConnectStateCallback().onConnectStateCallback(macAddr, Constant.CONNECTING);
            return Constant.SUCCESS;
        } else if (state == Constant.ADD_LIST_FAIL_EXIST_CONNECTINOF) {
            Log.e(TAG, macAddr + ":  ALREDY_CONNECT  ADD_LIST_FAIL_EXIST_CONNECTINOF");
            return Constant.ALREDY_CONNECT;
        } else {
            Log.e(TAG, "MAX_CONNECT");
            return Constant.MAX_CONNECT;
        }
    }


    public void disconnectAll() {
        if (mConnectInfoList == null) {
            Log.i(TAG, "disconnectAll mConnectInfoList == null");
            return;
        }
        synchronized (BleManager.mConnectInfoList) {
            Iterator<ConnectInfo> iterator = mConnectInfoList.iterator();
            while (iterator.hasNext()) {
                ConnectInfo connectInfo = (ConnectInfo) iterator.next();
                disconnect(connectInfo.getMacAddr());
            }
        }
    }

    public int disconnect(String macAddr) {
        if (!BluetoothAdapter.checkBluetoothAddress(macAddr)) {
            Log.e(TAG, macAddr + ":  MacAddress error");
            return Constant.FAIL_PARAMETER;
        }

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, macAddr + ":  FAIL_ADAPTER");
            return Constant.FAIL_ADAPTER;
        }

        if (alreadyDisConnect(macAddr)) {
            Log.e(TAG, macAddr + ":  ALREDY_DISCONNECT");
            return Constant.ALREADY_DISCONNECT;
        }

        synchronized (this) {
            ConnectInfo connectInfo = BleManager.getConnectInfo(mConnectInfoList, macAddr);
            if (connectInfo == null) {
                Log.e(TAG, macAddr + ":  not in the list");
                return Constant.FAIL;
            }

            BluetoothGatt bluetoothGatt = connectInfo.getmBluetoothGatt();

            if (bluetoothGatt == null) {
                Log.e(TAG, macAddr + ":  ALREDY_DISCONNECT not in list");
                return Constant.ALREADY_DISCONNECT;
            }

            connectInfo.setState(Constant.DISCONNECTING);
            connectInfo.getmConnectStateCallback().onConnectStateCallback(macAddr, Constant.DISCONNECTING);
            TimeOut disconncectTimeOut = new TimeOut(mConnectInfoList,
                    TimeOut.TPYE_DISCONNECT, macAddr);
            connectInfo.setDisconnectTimeOut(disconncectTimeOut);
            disconncectTimeOut.startTimeout();
            bluetoothGatt.disconnect();
            Log.i(TAG, "disconnecting");
        }
        return Constant.SUCCESS;
    }

    private ConnectInfo createConnectInfo(String macAddr, ConnectStateCallback connectStateCallback,
                                          BluetoothGatt bluetoothGatt) {
        ConnectInfo connectInfo = new ConnectInfo();
        connectInfo.setMacAddr(macAddr);
        connectInfo.setState(Constant.CONNECTING);
        connectInfo.setmBluetoothGatt(bluetoothGatt);
        connectInfo.setmConnectStateCallback(connectStateCallback);
        TimeOut connectTimeOut = new TimeOut(mConnectInfoList,
                TimeOut.TPYE_CONNECT, macAddr);
        connectInfo.setConnectTimeOut(connectTimeOut);
        connectTimeOut.startTimeout();
        return connectInfo;
    }


    private boolean alreadyConnect(String macAddr) {
        ConnectInfo connectInfo;
        if (mConnectInfoList == null) {
            Log.e(TAG, "mConnectInfoList == null");
            return false;
        }
        for (int i = 0; i < mConnectInfoList.size(); i++) {
            connectInfo = mConnectInfoList.get(i);
            if (connectInfo.getMacAddr().equals(macAddr)) {
                return true;
            }
        }
        return false;
    }


    private boolean alreadyDisConnect(String macAddr) {
        ConnectInfo connectInfo;
        for (int i = 0; i < mConnectInfoList.size(); i++) {
            connectInfo = mConnectInfoList.get(i);
            if (connectInfo.getMacAddr().equals(macAddr)) {
                int state = connectInfo.getState();
                if (state == Constant.DISCONNECTING
                        || state == Constant.DISCONNECTED
                        || state == Constant.DISCONNECT_TIMEOUT) {
                    return true;
                }
                break;
            }
        }
        return false;
    }


    BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status,
                                            int newState) {
            // TODO Auto-generated method stub
            super.onConnectionStateChange(gatt, status, newState);
            Log.e(TAG, "onConnectionStateChange" + gatt.getDevice().getAddress() + ":newState = " + newState + "   status=" + status);

            ConnectInfo connectInfo = BleManager.getConnectInfo(mConnectInfoList,
                    gatt.getDevice().getAddress());
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (connectInfo != null) {
                    if (connectInfo.getConnectTimeOut() != null) {
                        connectInfo.getConnectTimeOut().stopTimeout();
                        connectInfo.setConnectTimeOut(null);
                    }
                    connectInfo.setState(Constant.CONNECTED);
                    connectInfo.getmConnectStateCallback()
                            .onConnectStateCallback(gatt.getDevice().getAddress(),
                                    Constant.CONNECTED);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Log.e(TAG, "onConnectionStateChange: MTU :" + mMtu);
                        if(mMtu > Constant.DEFAULT_MTU) {
                            gatt.requestMtu(mMtu);
                        }
                    }

                    Log.i(TAG, "discovery services");
                    gatt.discoverServices();
                    connectInfo.setState(Constant.DISCOVERY_SERVICE_ING);
                    connectInfo.getmConnectStateCallback()
                            .onConnectStateCallback(gatt.getDevice().getAddress(),
                                    Constant.DISCOVERY_SERVICE_ING);
                    TimeOut discoveryTimeOut = new TimeOut(mConnectInfoList,
                            TimeOut.TPYE_SERVICE_DISCOVERY, connectInfo.getMacAddr());
                    connectInfo.setDiscoveryServiceTimeOut(discoveryTimeOut);
                    discoveryTimeOut.startTimeout();
                } else {
                    gatt.disconnect();
                    gatt.close();
                    Log.i(TAG, "connect gatt close not in list");
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (connectInfo != null) {
                    mBleManager.mBleDataTransition.clearSend(connectInfo.getMacAddr());
                    TimeOut conncetTimeout = connectInfo.getConnectTimeOut();
                    if (conncetTimeout != null) {
                        conncetTimeout.stopTimeout();
                        conncetTimeout = null;
                        connectInfo.setConnectTimeOut(null);
                    }

                    TimeOut disconncetTimeout = connectInfo.getDisconnectTimeOut();
                    if (disconncetTimeout != null) {
                        disconncetTimeout.stopTimeout();
                        conncetTimeout = null;
                        connectInfo.setDisconnectTimeOut(null);
                    }
                    connectInfo.setState(Constant.DISCONNECTED);
                    connectInfo.getmConnectStateCallback()
                            .onConnectStateCallback(gatt.getDevice().getAddress(),
                                    Constant.DISCONNECTED);
                } else {
                    gatt.close();
                    Log.i(TAG, "disconnect gatt close not in list");
                }
            } else {
                Log.e(TAG, "unknow state");
            }


        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // TODO Auto-generated method stub
            super.onServicesDiscovered(gatt, status);
            Log.e(TAG, "onServicesDiscovered" + gatt.getDevice().getAddress() + " status=" + status);
            ConnectInfo connectInfo = BleManager.getConnectInfo(mConnectInfoList,
                    gatt.getDevice().getAddress());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (connectInfo != null) {
                    if (connectInfo.getDiscoveryServiceTimeOut() != null) {
                        connectInfo.getDiscoveryServiceTimeOut().stopTimeout();
                        connectInfo.setDisconnectTimeOut(null);
                    }

                    connectInfo.setState(Constant.DISCOVERY_SERVICE_OK);
                    connectInfo.getmConnectStateCallback()
                            .onConnectStateCallback(gatt.getDevice().getAddress(),
                                    Constant.DISCOVERY_SERVICE_OK);
                }
            } else {
                if (connectInfo != null) {
                    if (connectInfo.getDiscoveryServiceTimeOut() != null) {
                        connectInfo.getDiscoveryServiceTimeOut().stopTimeout();
                        connectInfo.setDisconnectTimeOut(null);
                    }
                    connectInfo.setState(Constant.DISCOVERY_SERVICE_FAIL);
                    connectInfo.getmConnectStateCallback()
                            .onConnectStateCallback(gatt.getDevice().getAddress(),
                                    Constant.DISCOVERY_SERVICE_FAIL);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            // TODO Auto-generated method stub
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, " onCharacteristicRead:"
                        + BleUtils.byteArrayToCharString(characteristic.getValue()));
                if (mReciveDataCallback == null) {
                    Log.e(TAG, "mReciveDataCallback is null");
                    return;
                }
                mReciveDataCallback.onReceiveData(
                        gatt.getDevice().getAddress(), characteristic);
            }
        }

        @SuppressLint("NewApi")
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            // TODO Auto-generated method stub
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                blocking = false;
            } else {
                Log.e(TAG, "error_status:" + status);
            }

            Log.i(TAG, " onCharacteristicWrite: " + BleUtils.byteArrayToString(characteristic.getValue()));

            if (mBleDataTransition == null) {
                return;
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (!mBleDataTransition.isWriteDataSame(characteristic.getValue())) {
                    Log.i(TAG, " onCharacteristicWrite: data no same");
                    mBleDataTransition.interruptThread(gatt.getDevice().getAddress(), false);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        gatt.abortReliableWrite();
                    }
                } else {
                    Log.i(TAG, " onCharacteristicWrite: executeReliableWrite");
                    gatt.executeReliableWrite();
                }
            } else {
                Log.i(TAG, " onCharacteristicWrite: not GATT_SUCCESS");
                mBleDataTransition.interruptThread(gatt.getDevice().getAddress(), false);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            // TODO Auto-generated method stub
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i(TAG, " onCharacteristicChanged:" + BleUtils.byteArrayToString(characteristic.getValue()));
            if (mReciveDataCallback == null) {
                Log.e(TAG, "mReciveDataCallback is null");
                return;
            }
            mReciveDataCallback.onReceiveData(gatt.getDevice().getAddress(),
                    characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
            // TODO Auto-generated method stub
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            // TODO Auto-generated method stub
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.e(TAG, "onDescriptorWrite:" + status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            // TODO Auto-generated method stub
            super.onReliableWriteCompleted(gatt, status);
            Log.i(TAG, " onReliableWriteCompleted: status=" + status);

            if (mBleDataTransition == null) {
                return;
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBleDataTransition.interruptThread(gatt.getDevice().getAddress(), true);
            } else {
                Log.i(TAG, " onReliableWriteCompleted: not GATT_SUCCESS");
                mBleDataTransition.interruptThread(gatt.getDevice().getAddress(), false);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            // TODO Auto-generated method stub
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status){
            super.onMtuChanged(gatt,mtu,status);
            Log.e(TAG, " onMtuChanged: mtu=" + mtu);
            mMtu = mtu - 3;
        }
    };

    public void closeGatt(String macAddr) {
        ConnectInfo connectInfo = BleManager.getConnectInfo(mConnectInfoList, macAddr);
        BluetoothGatt gatt = connectInfo.getmBluetoothGatt();
        BleManager.removeConnectInfo(mConnectInfoList, gatt.getDevice().getAddress());
        gatt.close();
    }
}
