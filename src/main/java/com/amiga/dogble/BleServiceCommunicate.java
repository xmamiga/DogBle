package com.amiga.dogble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.amiga.dogble.util.ConnectInfo;
import com.amiga.dogble.util.Constant;

import java.util.List;
import java.util.UUID;

/**
 * Date: 2018/10/13-11:30
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class BleServiceCommunicate {
    private static final String TAG = "BleServiceCommunicate";

    public static final UUID CCC = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");

    BluetoothAdapter mBluetoothAdapter;
    BleManager mBleManager;
    List<ConnectInfo> mConnectInfoList;

    public BleServiceCommunicate(BleManager bleManager){
        mBleManager = bleManager;
    }

    protected  void init(BluetoothAdapter bluetoothAdapter,
                         List< ConnectInfo > connectInfoList){
        mBluetoothAdapter = bluetoothAdapter;
        mConnectInfoList = connectInfoList;
    }

    public BluetoothGattCharacteristic getChrateristic(String macAddr,
                                                       UUID service, UUID characteristic){
        ConnectInfo connectInfo = BleManager.getConnectInfo(mConnectInfoList, macAddr);
        if(connectInfo == null){
            Log.e(TAG,"connectInfoList not cotain "+macAddr);
            return null;
        }

        if(connectInfo.getState()!= Constant.DISCOVERY_SERVICE_OK){
            Log.e(TAG,"connectInfoList connect state is not DISCOVERY_SERVICE_OK"+macAddr);
            return null;
        }

        BluetoothGattService bluetoothGattService = connectInfo.getmBluetoothGatt().getService(service);
        if(bluetoothGattService == null){
            Log.e(TAG,"service not found "+macAddr);
            return null;
        }
        BluetoothGattCharacteristic cha = bluetoothGattService.getCharacteristic(characteristic);
        if(cha == null){
            Log.e(TAG,"Characteristic not found "+macAddr);
            return null;
        }

        return cha;
    }


    public boolean  setNotificationCharateristic(String macAddr, UUID service, UUID notifycharacteristic){
        ConnectInfo connectInfo = BleManager.getConnectInfo(mConnectInfoList, macAddr);
        if(connectInfo == null){
            Log.e(TAG,"connectInfoList not cotain "+macAddr);
            return false;
        }

        if(connectInfo.getState()!= Constant.DISCOVERY_SERVICE_OK){
            Log.e(TAG,"connectInfoList connect state is not DISCOVERY_SERVICE_OK"+macAddr);
            return false;
        }

        BluetoothGattService bluetoothGattService = connectInfo.getmBluetoothGatt().getService(service);
        if(bluetoothGattService == null){
            Log.e(TAG,"service not found "+macAddr);
            return false;
        }

        BluetoothGattCharacteristic cha = bluetoothGattService.getCharacteristic(notifycharacteristic);
        if(cha == null){
            Log.e(TAG,"Characteristic not found "+macAddr);
            return false;
        }

        boolean result = setCharacteristicNotification(connectInfo.getmBluetoothGatt(),
                cha, true);

        return result;
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic
     *            Characteristic to act on.
     * @param enabled
     *            If true, enable notification. False otherwise.
     */
    public boolean setCharacteristicNotification(BluetoothGatt bluetoothGatt,
                                                 BluetoothGattCharacteristic characteristic, boolean enabled) {
        if(!bluetoothGatt.setCharacteristicNotification(characteristic, enabled))
            return false;
//		Log.e(TAG, "bluetoothGatt,setCharacteristicNotification status="+true);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCC);
        // This is specific to Heart Rate Measurement.
        if (enabled) {
            if(!descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)){
                return false;
            }
        }
        else
        {
            if(descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)){
                return false;
            }
        }
        return bluetoothGatt.writeDescriptor(descriptor);
    }
}
