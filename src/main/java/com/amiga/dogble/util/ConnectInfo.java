package com.amiga.dogble.util;

import android.bluetooth.BluetoothGatt;

import com.amiga.dogble.callback.ConnectStateCallback;

/**
 * Date: 2018/10/12-19:46
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class ConnectInfo {

    String macAddr;
    int state = Constant.CONNECT_IDLE;
    BluetoothGatt mBluetoothGatt;
    ConnectStateCallback mConnectStateCallback;
    TimeOut connectTimeOut;
    TimeOut disconnectTimeOut;
    TimeOut discoveryServiceTimeOut;


    /**
     * @return the macAddr
     */
    public String getMacAddr() {
        return macAddr;
    }

    /**
     * @param macAddr the macAddr to set
     */
    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }

    /**
     * @return the state
     */
    public int getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * @return the mBluetoothGatt
     */
    public BluetoothGatt getmBluetoothGatt() {
        return mBluetoothGatt;
    }

    /**
     * @param mBluetoothGatt the mBluetoothGatt to set
     */
    public void setmBluetoothGatt(BluetoothGatt mBluetoothGatt) {
        this.mBluetoothGatt = mBluetoothGatt;
    }

    /**
     * @return the mConnectStateCallback
     */
    public ConnectStateCallback getmConnectStateCallback() {
        return mConnectStateCallback;
    }

    /**
     * @param mConnectStateCallback the mConnectStateCallback to set
     */
    public void setmConnectStateCallback(ConnectStateCallback mConnectStateCallback) {
        this.mConnectStateCallback = mConnectStateCallback;
    }

    /**
     * @return the connectTimeOut
     */
    public TimeOut getConnectTimeOut() {
        return connectTimeOut;
    }

    /**
     * @param connectTimeOut the connectTimeOut to set
     */
    public void setConnectTimeOut(TimeOut connectTimeOut) {
        this.connectTimeOut = connectTimeOut;
    }

    /**
     * @return the disconnectTimeOut
     */
    public TimeOut getDisconnectTimeOut() {
        return disconnectTimeOut;
    }

    /**
     * @param disconnectTimeOut the disconnectTimeOut to set
     */
    public void setDisconnectTimeOut(TimeOut disconnectTimeOut) {
        this.disconnectTimeOut = disconnectTimeOut;
    }

    /**
     * @return the discoveryServiceTimeOut
     */
    public TimeOut getDiscoveryServiceTimeOut() {
        return discoveryServiceTimeOut;
    }

    /**
     * @param discoveryServiceTimeOut the discoveryServiceTimeOut to set
     */
    public void setDiscoveryServiceTimeOut(TimeOut discoveryServiceTimeOut) {
        this.discoveryServiceTimeOut = discoveryServiceTimeOut;
    }


    @Override
    public String toString() {
        // TODO Auto-generated method stub
        String result = "";
        result = "macAddr:" + macAddr
                + "  state:" + state
                + "  mBluetoothGatt:" + mBluetoothGatt
                + "  mConnectStateCallback:" + mConnectStateCallback
                + "  connectTimeOut:" + connectTimeOut
                + "  disconnectTimeOut:" + disconnectTimeOut
                + "  discoveryServiceTimeOut:" + discoveryServiceTimeOut;
        return result;
    }

}
