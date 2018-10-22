package com.cchip.maddogbt.ble;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Date: 2018/10/16-16:07
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class CommunicationChannelBean {
    // boolean mNotificationSuccess;
    BluetoothGattCharacteristic configWriteCharacteristic;

    public CommunicationChannelBean(
            BluetoothGattCharacteristic configWriteCharacteristic) {
        // TODO Auto-generated constructor stub
        this.configWriteCharacteristic = configWriteCharacteristic;
    }

    public BluetoothGattCharacteristic getConfigWriteCharacteristic() {
        return configWriteCharacteristic;
    }

    public void setConfigWriteCharacteristic(
            BluetoothGattCharacteristic configWriteCharacteristic) {
        this.configWriteCharacteristic = configWriteCharacteristic;
    }
}
