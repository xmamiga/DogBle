package com.amiga.dogble.callback;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Date: 2018/10/12-19:32
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public interface ReceiveDataCallback {
    void onReceiveData(String macAddr,
                       BluetoothGattCharacteristic characteristic);
}
