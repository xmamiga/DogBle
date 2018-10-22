package com.cchip.maddogbt.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.amiga.dogble.BlePublicApi;
import com.amiga.dogble.Communciation;


/**
 * Date: 2018/10/16-16:08
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class BleApiConfig extends BlePublicApi {
    private static final String TAG = "BlePublicApi";

    private static final boolean NEED_AUTO_RECONNECT = false;
    private static final boolean NEED_CONNECT_STATUS_BROADCAST = true;

    public ProtocolConfig mProtocol;
    //	AutoReConnect mAutoReConnect;
    Communciation<CommunicationChannelBean> mCommunciation;


    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BleApiConfig getService() {
            return BleApiConfig.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        Log.e(TAG, "onbind");
        return binder;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        super.setNeedAutoReConnect(NEED_AUTO_RECONNECT);
        super.setNeedConnectStatusBroadcast(NEED_CONNECT_STATUS_BROADCAST);
        Log.e(TAG, "oncreat");
        mCommunciation = new CommunciationImp(this);
        mProtocol = new ProtocolConfig(this);
    }

    @Override
    public void commucateInit(String addr) {
        // TODO Auto-generated method stub
        mCommunciation.commucateInit(addr);
    }

    @Override
    public void commucateInitAall() {
        // TODO Auto-generated method stub
        mCommunciation.commucateInitAall();
    }

    @Override
    public boolean getCommunication(String addr) {
        // TODO Auto-generated method stub
        return mCommunciation.getCommunication(addr);
    }

    @Override
    public boolean isCommunicte(String addr) {
        // TODO Auto-generated method stub
        return mCommunciation.isCommunicte(addr);
    }

    @Override
    public void sendCmdAfterConnected(String addr) {
        // TODO Auto-generated method stub
        mProtocol.sendCmdAfterConnected(addr);
    }

    @Override
    public void prasedata(String macAddr, BluetoothGattCharacteristic characteristic) {
        // TODO Auto-generated method stub
        mProtocol.prasedata(macAddr, characteristic);
    }

    public BluetoothGattCharacteristic getCharBlock(String macAddr) {
        BluetoothGattCharacteristic mCharBlock;
        mCharBlock = getWriteCharateristic(macAddr,
                ProtocolConfig.otaService_UUID,
                ProtocolConfig.otaBlockRequest_UUID);
        return mCharBlock;
    }

    public BluetoothGattCharacteristic getCharIdentify(String macAddr) {
        BluetoothGattCharacteristic mCharIdentify;
        mCharIdentify = getWriteCharateristic(macAddr,
                ProtocolConfig.otaService_UUID,
                ProtocolConfig.otaImageNotify_UUID);
        boolean mCharIdenotify = setCharateristicNotification(macAddr,
                ProtocolConfig.otaService_UUID,
                ProtocolConfig.otaImageNotify_UUID);
        return mCharIdentify;
    }

    public BluetoothGattCharacteristic getCharConnReq(String macAddr) {
        BluetoothGattCharacteristic mCharConnReq;
        mCharConnReq = getWriteCharateristic(macAddr,
                ProtocolConfig.otaService_UUID,
                ProtocolConfig.otaBlockRequest_UUID);
        return mCharConnReq;
    }
}
