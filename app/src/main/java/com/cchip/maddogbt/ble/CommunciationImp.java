package com.cchip.maddogbt.ble;

import android.util.Log;

import com.amiga.dogble.Communciation;
import com.amiga.dogble.util.Constant;

import java.util.HashMap;
import java.util.Map;

/**
 * Date: 2018/10/16-16:10
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class CommunciationImp implements
        Communciation<CommunicationChannelBean> {
    private static final String TAG = "Communciation";

    BleApiConfig mBleApiBtDevice;

    private Map<String, CommunicationChannelBean> communicationChannelMap = new HashMap<String, CommunicationChannelBean>();

    public CommunciationImp(BleApiConfig bleApiBtDevice) {
        // TODO Auto-generated constructor stub
        final BleApiConfig bleApiBtDvice1 = bleApiBtDevice;
        mBleApiBtDevice = bleApiBtDvice1;
    }

    private synchronized void addCommunicationChannel(String addr,
                                                      CommunicationChannelBean channel) {
        if (addr == null || channel == null) {
            Log.e(TAG,
                    "addCommunicationChannel addr == null || channel == null");
            return;
        }

        communicationChannelMap.put(addr, channel);
    }

    private synchronized void removeCommunicationChannel(String addr) {
        if (addr == null) {
            Log.e(TAG, "removeCommunicationChannel addr == null");
        }

        communicationChannelMap.remove(addr);
    }

    public synchronized CommunicationChannelBean getCommunicationChannel(
            String addr) {
        if (addr == null) {
            Log.e(TAG, "removeCommunicationChannel addr == null");
        }

        return communicationChannelMap.get(addr);
    }

    private synchronized void removeAllCommunicationChannel() {
        communicationChannelMap.clear();
    }

    public void commucateInitAall() {
        removeAllCommunicationChannel();
    }

    public void commucateInit(String addr) {
        removeCommunicationChannel(addr);
    }

    public boolean getCommunication(final String addr) {
        mBleApiBtDevice.setCharateristicNotification(addr,
                ProtocolConfig.otaService_UUID,
                ProtocolConfig.otaImageNotify_UUID);
        return true;
    }

    public boolean isCommunicte(String macAddr) {
        if (mBleApiBtDevice.getBleAdapterState() == Constant.BLUETHOOTH_STATE_ON) {
            int state = mBleApiBtDevice.getConnectState(macAddr);
            // Log.i(TAG, "getConnectState state ="+state);
            if (state == Constant.DISCOVERY_SERVICE_OK) {
                return true;
            }
        }
        return false;
    }
}
